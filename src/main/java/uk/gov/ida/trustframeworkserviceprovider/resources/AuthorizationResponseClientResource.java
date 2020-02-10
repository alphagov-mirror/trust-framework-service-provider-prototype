package uk.gov.ida.trustframeworkserviceprovider.resources;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import net.minidev.json.JSONObject;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.dto.OidcResponseBody;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnResponseValidationService;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.TokenRequestService;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.QueryParameterHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Path("/formPost")
public class AuthorizationResponseClientResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationResponseClientResource.class);

    private final TokenRequestService tokenRequestService;
    private final AuthnResponseValidationService authnResponseValidationService;
    private final RedisService redisService;
    private final TrustFrameworkServiceProviderConfiguration configuration;

    public AuthorizationResponseClientResource(
            TokenRequestService tokenRequestService,
            AuthnResponseValidationService authnResponseValidationService,
            RedisService redisService,
            TrustFrameworkServiceProviderConfiguration configuration) {
        this.tokenRequestService = tokenRequestService;
        this.authnResponseValidationService = authnResponseValidationService;
        this.redisService = redisService;
        this.configuration = configuration;
    }

    @POST
    @Path("/validateAuthenticationResponse")
    @Consumes(MediaType.APPLICATION_JSON)
    public String validateAuthenticationResponse(OidcResponseBody oidcResponseBody) throws java.text.ParseException, ParseException, IOException, JOSEException {
        Map<String, String> authenticationParams = QueryParameterHelper.splitQuery(oidcResponseBody.getOidcResponse());
        String transactionID = authenticationParams.get("transactionID");

        Optional<String> errors = authnResponseValidationService.checkResponseForErrors(authenticationParams);

        if (errors.isPresent()) {
            JSONObject errorJsonObject = new JSONObject();
            errorJsonObject.put("Errors in Response on the TFSP", errors.toString());
            return errorJsonObject.toString();
        }

        String brokerDomain = configuration.getGovernmentBrokerURI();
        AuthorizationCode authorizationCode = authnResponseValidationService.handleAuthenticationResponse(authenticationParams, getClientID(), oidcResponseBody.getState(), oidcResponseBody.getNonce());
        String userInfoInJson = retrieveTokenAndUserInfo(authorizationCode, brokerDomain, transactionID);

        return userInfoInJson;
    }

    private String retrieveTokenAndUserInfo(AuthorizationCode authCode, String brokerDomain, String transactionID) throws IOException, JOSEException {


        TokenResponse tokenResponse = tokenRequestService.getTokens(authCode, getClientID(), brokerDomain);

        if (tokenResponse.indicatesSuccess()) {
            return tokenRequestService.getVerifiableCredential(
                    tokenResponse.toSuccessResponse().getTokens().getBearerAccessToken(),
                    brokerDomain,
                    transactionID);
        } else {
            return tokenResponse.toErrorResponse().toJSONObject().toString();
        }
//      UserInfo userInfo = tokenService.getUserInfo(tokens.getBearerAccessToken());
//      String userInfoToJson = userInfo.toJSONObject().toJSONString();

    }

    private ClientID getClientID() {
        String client_id = redisService.get("client-id");
        if (client_id != null) {
            return new ClientID(client_id);
        }
        return new ClientID();
    }
}
