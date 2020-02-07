package uk.gov.ida.trustframeworkserviceprovider.services.oidcclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.dto.Organisation;
import uk.gov.ida.trustframeworkserviceprovider.rest.Urls;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class RegistrationRequestService {

    private final RedisService redisService;
    private final TrustFrameworkServiceProviderConfiguration configuration;

    public RegistrationRequestService(RedisService redisService, TrustFrameworkServiceProviderConfiguration configuration) {
        this.redisService = redisService;
        this.configuration = configuration;
    }

    public String sendRegistrationRequest(String ssa, String privateKey, String brokerDomain, String brokerName, String clientToken) {

        SignedJWT signedJWT;
        try {
            signedJWT = SignedJWT.parse(ssa);
        } catch (ParseException e) {
            return "Unable to parse SSA:\n\n " + e;
        }

        String httpResponse;
        try {
            httpResponse = sendHttpRegistrationRequest(signedJWT, privateKey, brokerDomain, clientToken);
        } catch (IOException| JOSEException e) {
           return "There was an error with the Private Key used in the Registration Request:\n\n " + e;
        }

        String processedHttpResponse = processHttpRegistrationResponse(httpResponse, brokerName);

        return processedHttpResponse;
    }

    public List<Organisation> getListOfBrokersFromResponse(HttpResponse<String> responseBody) throws IOException {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        JSONArray jsonarray;
        try {
            jsonarray = (JSONArray) parser.parse(responseBody.body());
        } catch (net.minidev.json.parser.ParseException e) {
            throw new RuntimeException(e);
        }

        List<Organisation> orgList = new ArrayList<>();

        for (Object obj : jsonarray) {
            JSONObject jsonObj = (JSONObject) obj;
            ObjectMapper objectMapper = new ObjectMapper();
            Organisation org = objectMapper.readValue(jsonObj.toJSONString(), Organisation.class);
            orgList.add(org);
        }
        return orgList;
    }

    public HttpResponse<String> getRegisteredBrokersFromDirectory(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();

        try {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String sendHttpRegistrationRequest(SignedJWT jwt, String privateKey, String brokerDomain, String clientToken) throws IOException, JOSEException {
        URI uri = UriBuilder.fromUri(configuration.getGovernmentBrokerURI())
                .path(Urls.StubBrokerOPProvider.REGISTRATION_URI)
                .build();
        JWTClaimsSet registrationRequest = getRegistrationClaims(jwt.serialize(), brokerDomain);
        SignedJWT signedClientMetadata;

        signedClientMetadata = createSignedClientMetadata(registrationRequest, privateKey);


        HttpResponse<String> httpResponse = sendHttpRequest(uri, signedClientMetadata.serialize(), brokerDomain, clientToken);

        return httpResponse.body();
    }

    private String processHttpRegistrationResponse(String httpResponse, String brokerName) {
        if (httpResponse.equals("Failed Validation")) {
            return httpResponse;
        }

        JSONObject jsonObjectResponse;
        try {
            jsonObjectResponse = JSONObjectUtils.parse(httpResponse);
        } catch (ParseException e) {
            return "Unable to parse registration response:\n\n " + httpResponse;
        }

        if (jsonObjectResponse.get("client_id") != null) {
            saveClientID(jsonObjectResponse.get("client_id").toString());
        }

        return httpResponse;
    }

    private JWTClaimsSet getRegistrationClaims(String seralizedSoftwareStatement, String brokerDomain) {
        JWTClaimsSet registrationClaims = new JWTClaimsSet.Builder()
        .issuer(configuration.getSoftwareID())
        .issueTime(new Date())
        .expirationTime(new Date())
        .audience(brokerDomain)
        .jwtID(UUID.randomUUID().toString())
        .claim("redirect_uris", singletonList(UriBuilder.fromUri(configuration.getServiceProviderURI()).path(Urls.StubBrokerClient.REDIRECT_URI).build().toString()))
        .claim("token_endpoint_auth_method", "tls_client_auth")
        .claim("tls_client_auth_subject_dn", "This MUST contain the Distinguished name (DN) of the certificate that the Client will present to the OP token endpoint.")
        .claim("grant_types", singletonList("hybrid"))
        .claim("response_types", asList("code id_token", "code id_token token"))
        .claim("application_type", ApplicationType.WEB)
        .claim("id_token_signed_response_alg", JWSAlgorithm.RS256)
        .claim("request_object_signing_alg", JWSAlgorithm.RS256)
        .claim("software_statement", seralizedSoftwareStatement)
        .build();

        return registrationClaims;
    }

    private SignedJWT createSignedClientMetadata(JWTClaimsSet registrationRequestClaims, String privateKeyString) throws JOSEException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        privateKeyString = privateKeyString.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");
        String someString = privateKeyString.replaceAll("\\s+", "");

        String anotherString = "-----BEGIN RSA PRIVATE KEY-----\n" + someString + "\n-----END RSA PRIVATE KEY-----";

        PEMParser pemParser = new PEMParser(new StringReader(anotherString));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        PrivateKey privateKey = kp.getPrivate();

        JWSSigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(header, registrationRequestClaims);
        signedJWT.sign(signer);

        return signedJWT;
    }

    private HttpResponse<String> sendHttpRequest(URI uri, String postObject, String brokerDomain, String clientToken) {
        JSONObject jwtJson = new JSONObject();
        jwtJson.put("signed-jwt", postObject);
        jwtJson.put("destination-url", brokerDomain);

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Authorization", clientToken)
                .POST(HttpRequest.BodyPublishers.ofString(jwtJson.toJSONString()))
                .uri(uri)
                .build();

        try {
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveClientID(String clientID) {
        redisService.set("client-id", clientID);
    }
}

