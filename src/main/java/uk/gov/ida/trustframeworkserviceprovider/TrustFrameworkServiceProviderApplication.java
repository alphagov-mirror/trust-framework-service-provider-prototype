package uk.gov.ida.trustframeworkserviceprovider;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import uk.gov.ida.trustframeworkserviceprovider.configuration.TrustFrameworkServiceProviderConfiguration;
import uk.gov.ida.trustframeworkserviceprovider.resources.AuthorizationRequestClientResource;
import uk.gov.ida.trustframeworkserviceprovider.resources.AuthorizationResponseClientResource;
import uk.gov.ida.trustframeworkserviceprovider.resources.RegistrationRequestResource;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnRequestGeneratorService;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.AuthnResponseValidationService;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.RegistrationRequestService;
import uk.gov.ida.trustframeworkserviceprovider.services.oidcclient.TokenRequestService;
import uk.gov.ida.trustframeworkserviceprovider.services.shared.RedisService;

public class TrustFrameworkServiceProviderApplication extends Application<TrustFrameworkServiceProviderConfiguration> {

    public static void main(String[] args) throws Exception {
        new TrustFrameworkServiceProviderApplication().run(args);
    }

    @Override
    public void run(TrustFrameworkServiceProviderConfiguration configuration, Environment environment) {
        RedisService redisService = new RedisService(configuration);

        registerOidcClientResources(environment, configuration, redisService);
    }

    @Override
    public void initialize(final Bootstrap<TrustFrameworkServiceProviderConfiguration> bootstrap) {
        bootstrap.addBundle(new ViewBundle<>());
        bootstrap.addBundle(new AssetsBundle("/stylesheets", "/stylesheets", null, "css"));
        bootstrap.addBundle(new AssetsBundle("/javascript", "/javascript", null, "js"));
        bootstrap.addBundle(new AssetsBundle("/assets/fonts", "/assets/fonts", null, "fonts"));
        bootstrap.addBundle(new AssetsBundle("/assets/images", "/assets/images", null, "images"));
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    private void registerOidcClientResources(Environment environment, TrustFrameworkServiceProviderConfiguration configuration, RedisService redisService) {
        TokenRequestService tokenRequestService = new TokenRequestService(configuration, redisService);
        AuthnRequestGeneratorService authnRequestGeneratorService = new AuthnRequestGeneratorService(redisService);
        AuthnResponseValidationService authResponseService = new AuthnResponseValidationService(tokenRequestService);
        RegistrationRequestService registrationRequestService = new RegistrationRequestService(redisService, configuration);

        environment.jersey().register(new AuthorizationRequestClientResource(configuration, authnRequestGeneratorService, redisService));
        environment.jersey().register(new JsonProcessingExceptionMapper(true));
        environment.jersey().register(new RegistrationRequestResource(registrationRequestService, redisService, configuration));
        environment.jersey().register(new AuthorizationResponseClientResource(tokenRequestService, authResponseService, redisService, configuration));
    }
}
