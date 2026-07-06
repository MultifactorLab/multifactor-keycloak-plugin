package ru.multifactor.keycloak.auth.spi.mf;

import org.json.JSONObject;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static ru.multifactor.keycloak.auth.spi.mf.MultifactorAuthenticatorFactory.PROP_FEDERATION_MAP;
import static ru.multifactor.keycloak.auth.spi.mf.MultifactorAuthenticatorFactory.PROP_KEY;
import static ru.multifactor.keycloak.auth.spi.mf.MultifactorAuthenticatorFactory.PROP_SECRET;

public final class FederationCredentialsResolver {

    public static final class Credentials {
        public final String apiKey;
        public final String apiSecret;

        Credentials(String apiKey, String apiSecret) {
            this.apiKey = apiKey;
            this.apiSecret = apiSecret;
        }
    }

    private FederationCredentialsResolver() {
    }

    public static Credentials resolve(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String defaultKey = "";
        String defaultSecret = "";
        String federationMapJson = null;

        if (config != null && config.getConfig() != null) {
            defaultKey = configValue(config.getConfig().get(PROP_KEY));
            defaultSecret = configValue(config.getConfig().get(PROP_SECRET));
            federationMapJson = configValue(config.getConfig().get(PROP_FEDERATION_MAP));
            if (federationMapJson.isEmpty()) {
                federationMapJson = null;
            }
        }

        String federationName = federationName(context);
        if (federationName != null && federationMapJson != null) {
            Credentials federationCredentials = lookupFederationMap(federationMapJson, federationName);
            if (federationCredentials != null) {
                return federationCredentials;
            }
        }

        return new Credentials(defaultKey, defaultSecret);
    }

    static String federationName(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            return null;
        }
        String federationLink = user.getFederationLink();
        if (federationLink == null) {
            return null;
        }
        RealmModel realm = context.getRealm();
        ComponentModel component = realm.getComponent(federationLink);
        if (component == null) {
            return null;
        }
        return component.getName();
    }

    static Credentials lookupFederationMap(String mapJson, String federationName) {
        try {
            JSONObject map = new JSONObject(mapJson);
            if (!map.has(federationName)) {
                return null;
            }
            JSONObject entry = map.getJSONObject(federationName);
            String key = entry.optString("key", "").trim();
            String secret = entry.optString("secret", "").trim();
            if (key.isEmpty() || secret.isEmpty()) {
                return null;
            }
            return new Credentials(key, secret);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String configValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return "null".equals(text) ? "" : text;
    }
}
