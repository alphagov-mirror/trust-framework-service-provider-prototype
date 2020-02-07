package uk.gov.ida.trustframeworkserviceprovider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class OidcResponseBody {

    @NotNull
    @JsonProperty
    private String oidcResponse;

    @NotNull
    @JsonProperty
    private String state;

    @NotNull
    @JsonProperty
    private String nonce;


    public OidcResponseBody(
           String oidcResponse,
           String state,
           String nonce) {
        this.oidcResponse = oidcResponse;
        this.state = state;
        this.nonce = nonce;
    }

    public OidcResponseBody() {
    }

    public String getOidcResponse() {
        return oidcResponse;
    }

    public String getState() {
        return state;
    }

    public String getNonce() {
        return nonce;
    }

    @Override
    public String toString() {
        return "OidcResponseBody{" +
                "oidcResponse='" + oidcResponse + '\'' +
                ", state='" + state + '\'' +
                ", nonce='" + nonce + '\'' +
                '}';
    }
}

