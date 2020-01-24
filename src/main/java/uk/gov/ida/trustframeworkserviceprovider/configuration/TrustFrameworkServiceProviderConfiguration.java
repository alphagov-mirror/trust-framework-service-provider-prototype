package uk.gov.ida.trustframeworkserviceprovider.configuration;

import io.dropwizard.Configuration;

public class TrustFrameworkServiceProviderConfiguration extends Configuration {

    private String serviceProviderURI;
    private String redisURI;
    private boolean local;
    private String softwareID;
    private String directoryURI;
    private String middlewareURI;
    private String scheme;
    private String governmentBrokerURI;
    private String rpURI;
    private String orgID;

    public String getServiceProviderURI() {
        return serviceProviderURI;
    }

    public String getRedisURI() {
        return redisURI;
    }

    public boolean isLocal() {
        return local;
    }

    public String getSoftwareID() {
        return softwareID;
    }

    public String getDirectoryURI() {
        return directoryURI;
    }

    public String getMiddlewareURI() {
        return middlewareURI;
    }

    public String getScheme() {
        return scheme;
    }

    public String getGovernmentBrokerURI() {
        return governmentBrokerURI;
    }

    public String getRpURI() {
        return rpURI;
    }

    public String getOrgID() {
        return orgID;
    }
}
