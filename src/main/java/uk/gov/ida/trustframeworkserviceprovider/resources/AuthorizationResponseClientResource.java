package uk.gov.ida.trustframeworkserviceprovider.resources;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.dropwizard.views.View;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnResponseValidationService;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.TokenRequestService;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;
import uk.gov.ida.trustframeworkserviceprovider.views.RPResponseView;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.QueryParameterHelper;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Path("/formPost")
public class AuthorizationResponseClientResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationResponseClientResource.class);

    private final TokenRequestService tokenRequestService;
    private final AuthnResponseValidationService authnResponseValidationService;
    private final RedisService redisService;

    public AuthorizationResponseClientResource(
            TokenRequestService tokenRequestService,
            AuthnResponseValidationService authnResponseValidationService,
            RedisService redisService) {
        this.tokenRequestService = tokenRequestService;
        this.authnResponseValidationService = authnResponseValidationService;
        this.redisService = redisService;
    }

    @POST
    @Path("/validateAuthenticationResponse")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validateAuthenticationResponse(String postBody) throws java.text.ParseException, ParseException {
        Map<String, String> authenticationParams = QueryParameterHelper.splitQuery(postBody);
        String transactionID = authenticationParams.get("transactionID");
        String rpDomain = redisService.get(transactionID);
        LOG.info("RP Domain is :" + rpDomain);
        URI rpUri = UriBuilder.fromUri(rpDomain).build();

        if (postBody.isEmpty()) {
//            return new RPResponseView(rpUri, "Post Body is empty", Integer.toString(HttpStatus.SC_BAD_REQUEST));
        }

        Optional<String> errors = authnResponseValidationService.checkResponseForErrors(authenticationParams);

        if (errors.isPresent()) {
//            return new RPResponseView(rpUri, "Errors in Response: " + errors.get(), Integer.toString(HttpStatus.SC_BAD_REQUEST));
        }

        String brokerName = getBrokerName(transactionID);
        String brokerDomain = getBrokerDomain(transactionID);
        AuthorizationCode authorizationCode = authnResponseValidationService.handleAuthenticationResponse(authenticationParams, getClientID(brokerName));
        String userInfoInJson = retrieveTokenAndUserInfo(authorizationCode, brokerName, brokerDomain);

        return userInfoInJson;
    }

    private String retrieveTokenAndUserInfo(AuthorizationCode authCode, String brokerName, String brokerDomain) {

        OIDCTokens tokens = tokenRequestService.getTokens(authCode, getClientID(brokerName), brokerDomain);
//      UserInfo userInfo = tokenService.getUserInfo(tokens.getBearerAccessToken());
//      String userInfoToJson = userInfo.toJSONObject().toJSONString();

        return tokenRequestService.getVerifiableCredential(tokens.getBearerAccessToken(), brokerDomain);
    }

    private String getBrokerName(String transactionID) {
        return redisService.get(transactionID + "-brokername");
    }

    private String getBrokerDomain(String transactionID) {
        return redisService.get(transactionID + "-brokerdomain");
    }

    private ClientID getClientID(String brokerName) {
        String client_id = redisService.get(brokerName);
        if (client_id != null) {
            return new ClientID(client_id);
        }
        return new ClientID();
    }
}
