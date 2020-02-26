package uk.gov.ida.trustframeworkserviceprovider.services.oidcclient;

import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.ClaimsRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.ClaimRequirement;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;

import java.net.URI;
import java.util.Map;

public class AuthnRequestGeneratorService {

    private final RedisService redisService;

    public AuthnRequestGeneratorService(RedisService redisService) {
        this.redisService = redisService;
    }

    public AuthenticationRequest generateAuthenticationRequest(
            URI requestUri,
            URI redirectUri,
            ResponseType responseType,
            String transactionID,
            String serviceProviderURI,
            Map<String, String> claims) {
        Scope scope = new Scope("openid");

        State state = new State();
        Nonce nonce = new Nonce();
        ClaimsRequest claimsRequest = new ClaimsRequest();
        claims.forEach((claimName,requirement) ->
                claimsRequest.addUserInfoClaim(
                        claimName,
                        requirement.equals("essential") ? ClaimRequirement.ESSENTIAL : ClaimRequirement.VOLUNTARY)
        );

        AuthenticationRequest authenticationRequest = new AuthenticationRequest.Builder(
                responseType,
                scope, getClientID(), redirectUri)
                .responseMode(ResponseMode.FORM_POST)
                .endpointURI(requestUri)
                .state(state)
                .nonce(nonce)
                .claims(claimsRequest)
                .customParameter("transaction-id", transactionID)
                .customParameter("response-uri", serviceProviderURI)
                .build();

        return authenticationRequest;
    }

    private ClientID getClientID() {
        String client_id = redisService.get("client-id");
        if (client_id != null) {
            return new ClientID(client_id);
        }
        return new ClientID();
    }
}
