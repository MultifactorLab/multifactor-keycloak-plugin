# Keycloak plugin for multifactor

Keycloak plugin for multifactor authentication. 
Provides an authentication execution for keycloak that presents a Multifactor iframe, to be used after primary authentication. (https://multifactor.ru/)

## Build

You may need to modify the keycloak versions in the pom.xml to correspond to yours. Now used 18.0.0.

```
$ mvn clean install
```

## Install

```
# stop keycloak
$ cp target/keycloak-multifactor-spi-jar-with-dependencies.jar <keycloack dir>/providers
# run kc.bat build or kc.sh build from <keycloack dir>/bin
# start keycloak
```
## Configure

You need to add Multifactor as a trusted frame-able source to the Keycloak Content Security Policy.
Content-Security-Policy: `frame-src https://*.multifactor.ru/ 'self'; ...`

Since you can't modify the default Authentication Flows, make a copy of Browser. Add `Multifactor` as an execution under `Browser Forms`.

When you hit `Config` you can enter your Multifactor api key, api secret, and api url. 

Then make sure to bind your Copy of Browser flow to the Browser Flow (on the Bindings tab).

