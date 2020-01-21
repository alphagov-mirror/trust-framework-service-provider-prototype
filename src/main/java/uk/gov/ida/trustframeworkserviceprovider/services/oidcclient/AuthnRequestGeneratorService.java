package uk.gov.ida.trustframeworkserviceprovider.services.oidcclient;

import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;

import java.net.URI;

public class AuthnRequestGeneratorService {

    private final RedisService redisService;

    public AuthnRequestGeneratorService(RedisService redisService) {
        this.redisService = redisService;
    }

    public AuthenticationRequest generateAuthenticationRequest(
            URI requestUri,
            ClientID clientID,
            URI redirectUri,
            ResponseType responseType,
            String transactionID,
            String serviceProviderURI) {
        Scope scope = new Scope("openid");

        State state = new State();
        Nonce nonce = new Nonce();

        AuthenticationRequest authenticationRequest = new AuthenticationRequest.Builder(
                responseType,
                scope, clientID, redirectUri)
                .responseMode(ResponseMode.FORM_POST)
                .endpointURI(requestUri)
                .state(state)
                .nonce(nonce)
                .customParameter("transaction-id", transactionID)
                .customParameter("response-uri", serviceProviderURI)
                .build();

        redisService.set("state::" + state.getValue(), nonce.getValue());
        redisService.incr("nonce::" + nonce.getValue());

        return authenticationRequest;
    }
}
