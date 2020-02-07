package uk.gov.ida.trustframeworkserviceprovider.views;

import io.dropwizard.views.View;
import uk.gov.ida.trustframeworkserviceprovider.dto.Organisation;

import java.util.List;

public class RegistrationView extends View {
    private List<Organisation> brokers;

    public RegistrationView(List<Organisation> brokers) {
        super("registration.mustache");
        this.brokers = brokers;
    }

    public List<Organisation> getBrokers() {
        return brokers;
    }
}
