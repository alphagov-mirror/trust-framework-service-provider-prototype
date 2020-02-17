package uk.gov.ida.trustframeworkserviceprovider.resources;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.rest.Urls;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnRequestGeneratorService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Path("/formPost")
public class AuthorizationRequestClientResource {

    private final TrustFrameworkServiceProviderConfiguration configuration;
    private final AuthnRequestGeneratorService authnRequestGeneratorService;

    public AuthorizationRequestClientResource(
            TrustFrameworkServiceProviderConfiguration configuration,
            AuthnRequestGeneratorService authnRequestGeneratorService) {
        this.configuration = configuration;
        this.authnRequestGeneratorService = authnRequestGeneratorService;
    }

    @GET
    @Path("/generateAuthenticationRequest")
    public String generateAuthenticationRequest() {
        String transactionID = new ClientID().toString();
        String brokerDomain = configuration.getGovernmentBrokerURI();
        URI redirectUri = UriBuilder.fromUri(configuration.getRpURI()).path(Urls.RP.REDIRECT_URI).build();
        URI authorisationURI = UriBuilder.fromUri(brokerDomain).path(Urls.StubBrokerOPProvider.AUTHORISATION_ENDPOINT_FORM_URI).build();

        return authnRequestGeneratorService.generateAuthenticationRequest(
                        authorisationURI,
                        redirectUri,
                        new ResponseType(ResponseType.Value.CODE, OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.TOKEN),
                        transactionID,
                        configuration.getServiceProviderURI()).toURI().toString();
    }
}
