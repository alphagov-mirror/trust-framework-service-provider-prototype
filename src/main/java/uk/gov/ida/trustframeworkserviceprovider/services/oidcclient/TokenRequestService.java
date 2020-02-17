package uk.gov.ida.trustframeworkserviceprovider.services.oidcclient;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import net.minidev.json.JSONObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.rest.Urls;

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
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenRequestService {

    private final TrustFrameworkServiceProviderConfiguration configuration;

    public TokenRequestService(TrustFrameworkServiceProviderConfiguration configuration) {
        this.configuration = configuration;
    }

    public TokenResponse getTokens(AuthorizationCode authorizationCode, ClientID clientID, String idpDomain) {
        URI redirectURI = UriBuilder.fromUri(configuration.getServiceProviderURI()).path(Urls.StubBrokerClient.REDIRECT_URI).build();
        URI tokenURI = UriBuilder.fromUri(configuration.getGovernmentBrokerURI()).path(Urls.StubBrokerOPProvider.TOKEN).build();

        PrivateKeyJWT privateKeyJWT;
        try {
            privateKeyJWT = new PrivateKeyJWT(clientID, tokenURI, JWSAlgorithm.RS256, (RSAPrivateKey) getPrivateKey(),null, null);
        } catch (JOSEException e) {
            throw new RuntimeException("Unable to create PrivateKeyJWT", e);
        }

        Map<String, List<String>> customParams = new HashMap<>();
        customParams.put("destination-url", Collections.singletonList(idpDomain));

        TokenRequest tokenRequest = new TokenRequest(
                tokenURI,
                privateKeyJWT,
                new AuthorizationCodeGrant(authorizationCode, redirectURI),
                null,
                null,
                customParams
        );

        HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
        HTTPResponse httpResponse = sendHTTPRequest(httpRequest);

        try {
            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
            if (!tokenResponse.indicatesSuccess()) {
                return tokenResponse.toErrorResponse();
            }
            return tokenResponse.toSuccessResponse();
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse HTTP Response to Token Response", e);
        }
    }

    public UserInfo getUserInfo(BearerAccessToken bearerAccessToken, String idpDomain) {
        URI userInfoURI = UriBuilder.fromUri(idpDomain).path(Urls.StubBrokerOPProvider.USERINFO_URI).build();
        UserInfoRequest userInfoRequest = new UserInfoRequest(
                userInfoURI,
                bearerAccessToken);

        HTTPResponse httpResonse = sendHTTPRequest(userInfoRequest.toHTTPRequest());

        try {
            UserInfoResponse userInfoResponse = UserInfoResponse.parse(httpResonse);
            return userInfoResponse.toSuccessResponse().getUserInfo();
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse HTTP Response to UserInfoResponse", e);
        }
    }

    private HTTPResponse sendHTTPRequest(HTTPRequest request) {

        try {
            return request.send();
        } catch (IOException e) {
            throw new RuntimeException("Unable to send HTTP Request", e);
        }
    }

    public String getVerifiableCredential(BearerAccessToken bearerAccessToken, String brokerDomain, String transactionID) {
        URI userInfoURI = UriBuilder.fromUri(brokerDomain)
                .path(Urls.StubBrokerOPProvider.USERINFO_URI).build();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", bearerAccessToken.toAuthorizationHeader())
                .headers("transactionID", transactionID)
                .uri(userInfoURI)
                .build();

        HttpResponse<String> responseBody;
        try {
            responseBody = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return responseBody.body();
    }

    //For testing purposes
    private PrivateKey getPrivateKey() {
        Security.addProvider(new BouncyCastleProvider());

        URI directoryURI = UriBuilder.fromUri(configuration.getDirectoryURI()).path("keys").path(configuration.getOrgID()).build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(directoryURI)
                .build();

        JSONObject jsonResponse;

        try {
            HttpResponse<String>  httpResponse = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            jsonResponse = JSONObjectUtils.parse(httpResponse.body());
        } catch (IOException | InterruptedException | java.text.ParseException e) {
            throw new RuntimeException(e);
        }


        String responseString = jsonResponse.get("signing").toString();
        responseString = responseString.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "").replaceAll("\\s+", "");
        String anotherString = "-----BEGIN RSA PRIVATE KEY-----\n" + responseString + "\n-----END RSA PRIVATE KEY-----";
        PEMParser pemParser = new PEMParser(new StringReader(anotherString));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair kp;
        try {
            Object object = pemParser.readObject();
            kp = converter.getKeyPair((PEMKeyPair) object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return kp.getPrivate();
    }
}
