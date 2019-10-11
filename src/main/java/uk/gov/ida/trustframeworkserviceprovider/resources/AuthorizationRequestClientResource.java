package uk.gov.ida.trustframeworkserviceprovider.resources;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.rest.Urls;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnRequestGeneratorService;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Path("/formPost")
public class AuthorizationRequestClientResource {

    private final TrustFrameworkServiceProviderConfiguration configuration;
    private final AuthnRequestGeneratorService authnRequestGeneratorService;
    private final RedisService redisService;

    public AuthorizationRequestClientResource(
            TrustFrameworkServiceProviderConfiguration configuration,
            AuthnRequestGeneratorService authnRequestGeneratorService,
            RedisService redisService) {
        this.configuration = configuration;
        this.authnRequestGeneratorService = authnRequestGeneratorService;
        this.redisService = redisService;
    }

    @GET
    @Path("/serviceAuthenticationRequest")
    public Response formPostAuthenticationRequest() {
        String transactionID = new ClientID().toString();
        String brokerDomain = configuration.getGovernmentBrokerURI();
        String brokerName = "Post Office";
        redisService.set(transactionID, configuration.getRpURI());
        storeBrokerNameAndDomain(transactionID, brokerName, configuration.getGovernmentBrokerURI());
        URI redirectUri = UriBuilder.fromUri(configuration.getServiceProviderURI()).path(Urls.StubBrokerClient.REDIRECT_FORM_URI).build();
        URI authorisationURI = UriBuilder.fromUri(brokerDomain).path("/authorizeFormPost/authorize-sp").build();
        return Response
                .status(302)
                .location(authnRequestGeneratorService.generateAuthenticationRequest(
                        authorisationURI,
                        getClientID(brokerName),
                        redirectUri,
                        new ResponseType(ResponseType.Value.CODE, OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.TOKEN),
                        transactionID,
                        configuration.getServiceProviderURI())
                        .toURI())
                .build();
    }

    private void storeBrokerNameAndDomain(String transactionID, String brokerName, String brokerDomain) {
        redisService.set(transactionID + "-brokername", brokerName);
        redisService.set(transactionID + "-brokerdomain", brokerDomain);
    }

    private ClientID getClientID(String brokerName) {
        String client_id = redisService.get(brokerName);
        if (client_id != null) {
            return new ClientID(client_id);
        }
        return new ClientID();
    }
}
