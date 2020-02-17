package uk.gov.ida.trustframeworkserviceprovider.rest;

public interface Urls {

    interface StubBrokerClient {
        String REDIRECT_URI = "/authenticationCallback";
    }

    interface StubBrokerOPProvider {
        String USERINFO_URI = "/userinfo";
        String AUTHORISATION_ENDPOINT_FORM_URI = "/authorizeFormPost/authorize-sp";
        String REGISTRATION_URI = "/register";
        String TOKEN = "/token";
    }

    interface Directory {
        String REGISTERED_RPS = "/organisation/rp/";
    }

    interface IDP {
        String AUTHENTICATION_URI = "/authentication";
        String CREDENTIAL_URI = "/issue/jwt/credential";
    }

    interface RP {
        String REDIRECT_URI = "/authenticationResponse";
    }
}
