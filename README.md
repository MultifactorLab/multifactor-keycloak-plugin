# multifactor-keycloak-plugin
> Attention: The current version of the plugin only works with Keycloak, starting from version 23.0.1

Authentication execution plugin for Keycloak that adds <a href="https://multifactor.ru/" target="_blank">MultiFactor</a> into the authentication flow. Component uses Keycloak Service Provider Interface (SPI) to show user a MultiFactor iframe upon completion of primary authentication.

## Build

Modify `keycloak.version` in `pom.xml` to match to your specific Keycloak version (currently, version `22.0.1` is used), then build the component:

```
$ mvn clean install
```

## Install

```
$ cp <keycloack dir>/target/keycloak-multifactor-spi-jar-with-dependencies.jar <keycloack dir>/providers
# run kc.bat build or kc.sh build from <keycloack dir>/bin
# restart keycloak
```

## Configure

1. In <a href="https://admin.multifactor.ru/" target="_blank">MultiFactor</a> administration console, add new "Website" resource. Use `JwtHS256` access token format;

2. In KeyCloak "Realm Settings" -> "Security Defenses" -> "Content-Security-Policy" add MultiFactor as a trusted frame-able source: 
`frame-src https://*.multifactor.ru/ 'self';`

3. In KeyCloak "Authentication" -> "Flow" select "Browser" click "Action->Duplicate";

4. In KeyCloak "Authentication" -> "Flow" select "Copy of browser" and click "Add step" to "Copy of browser forms" and select `Multifactor`(Attention: "Multifactor" must be after "Username Password Form");

5. Press "Settings" for "Multifactor" and enter the following values:
  * API key: value from step 1;
  * API secret: value from step 1;
  * API URL: https://api.multifactor.ru.

6. Select `REQUIRED` under the Requirement column for "Multifactor". Save your configuration; 

7. In your Keycloak client's settings, in the "Advanced" -> "Authentication Flow Overrides" section, bind your "Copy of browser" to the Browser Flow. Alternatively, you can bind new flow globally: In "Authentication" -> "Flow" select "Copy of browser" and click "Action->Bind flow".