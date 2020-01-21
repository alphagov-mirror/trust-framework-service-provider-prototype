package uk.gov.ida.trustframeworkserviceprovider.rest;

public interface Urls {

    interface StubBrokerClient {
        String REDIRECT_FORM_URI = "/formPost/validateAuthenticationResponse";
        String REDIRECT_URI = "/authenticationCallback";
        String USER_INFO = "/userinfo";
        String IDP_AUTHENTICATION_RESPONE = "/formPost/idpAuthenticationResponse";
        String RESPONSE_FOR_BROKER = "/authorizeFormPost/response";
    }

    interface StubBrokerOPProvider {
        String AUTHORISATION_ENDPOINT_URI = "/authorize";
        String USERINFO_URI = "/userinfo";
        String AUTHORISATION_ENDPOINT_FORM_URI = "/authorizeFormPost/authorize";
    }

    interface Directory {
        String REGISTERED_IDPS = "/organisation/idp/";
        String REGISTERED_BROKERS = "/organisation/broker/";
        String REGISTERED_RPS = "/organisation/rp/";
    }

    interface Middleware {
        String REGISTRATION_URI = "/register";
        String TOKEN_URI = "/sender";
    }

    interface IDP {
        String AUTHENTICATION_URI = "/authentication";
        String CREDENTIAL_URI = "/issue/jwt/credential";
    }
}
