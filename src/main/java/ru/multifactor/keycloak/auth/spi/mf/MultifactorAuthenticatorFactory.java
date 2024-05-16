
package ru.multifactor.keycloak.auth.spi.mf;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class MultifactorAuthenticatorFactory implements AuthenticatorFactory{
    public static final String PROVIDER_ID = "multifactor-authenticator";
    private static final MultifactorAuthenticator SINGLETON = new MultifactorAuthenticator();
    public static final String PROP_KEY = "multifactor.key";
    public static final String PROP_SECRET = "multifactor.secret";
    public static final String PROP_APIURL = "multifactor.apiurl";
    public static final String PROP_BYPASS = "multifactor.bypass";
    public static final String PROP_USE_EMAIL = "multifactor.use_email";
    public static final String PROP_USER_ATTRIBUTE = "multifactor.user_attribute";


    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean  isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty key = new ProviderConfigProperty();
        key.setName(PROP_KEY);
        key.setLabel("Multifactor Api Key");
        key.setType(ProviderConfigProperty.STRING_TYPE);
        key.setHelpText("Api Key from Multifactor admin portal");
        configProperties.add(key);

        ProviderConfigProperty secret = new ProviderConfigProperty();
        secret.setName(PROP_SECRET);
        secret.setLabel("Multifactor Api Secret");
        secret.setType(ProviderConfigProperty.STRING_TYPE);
        secret.setHelpText("Api Secret from Multifactor admin portal");
        configProperties.add(secret);

        ProviderConfigProperty api_url = new ProviderConfigProperty();
        api_url.setName(PROP_APIURL);
        api_url.setLabel("Multifactor API URL");
        api_url.setType(ProviderConfigProperty.STRING_TYPE);
        api_url.setHelpText("Multifactor HTTP API URL");
        configProperties.add(api_url);

        ProviderConfigProperty bypass = new ProviderConfigProperty();
        bypass.setDefaultValue(true);
        bypass.setName(PROP_BYPASS);
        bypass.setLabel("Bypass");
        bypass.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        bypass.setHelpText("Enable bypass when api unreachable");
        configProperties.add(bypass);

        ProviderConfigProperty use_email = new ProviderConfigProperty();
        use_email.setDefaultValue(false);
        use_email.setName(PROP_USE_EMAIL);
        use_email.setLabel("Send an e-mail instead of a username");
        use_email.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        use_email.setHelpText("Send the keycloak e-mail attribute to the multifactor api instead of username");
        configProperties.add(use_email);

        ProviderConfigProperty user_attribute = new ProviderConfigProperty();
        user_attribute.setName(PROP_USER_ATTRIBUTE);
        user_attribute.setLabel("User attribute");
        user_attribute.setType(ProviderConfigProperty.STRING_TYPE);
        user_attribute.setHelpText("Send this user attribute to the multifactor api instead of username");
        configProperties.add(user_attribute);


    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getHelpText() {
        return "MFA provided by Multifactor";
    }

    @Override
    public String getDisplayType() {
        return "Multifactor";
    }

    @Override
    public String getReferenceCategory() {
        return "MFA";
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

}
