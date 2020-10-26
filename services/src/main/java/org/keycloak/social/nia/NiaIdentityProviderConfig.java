package org.keycloak.social.nia;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class NiaIdentityProviderConfig extends SAMLIdentityProviderConfig {

    private static final EidasLevel DEFAULT_EIDAS_LEVEL = EidasLevel.EIDAS1;

    NiaIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
        initialize();
    }

    NiaIdentityProviderConfig() {
        super();
        initialize();
    }

    private void initialize() {
        this.setValidateSignature(true);
    }

    boolean isIgnoreAbsentStateParameterLogout() {
        return Boolean.parseBoolean(getConfig().get("ignoreAbsentParameterLogout"));
    }

    EidasLevel getEidasLevel() {
        return EidasLevel.getOrDefault(getConfig().get(
                EidasLevel.EIDAS_LEVEL_PROPERTY_NAME),
                DEFAULT_EIDAS_LEVEL);
    }

    enum EidasLevel {
        EIDAS1, EIDAS2, EIDAS3;

        static final String EIDAS_LEVEL_PROPERTY_NAME = "eidas_values";

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        static EidasLevel getOrDefault(String eidasLevelName, EidasLevel defaultEidasLevel) {
            for (EidasLevel eidasLevel : EidasLevel.values()) {
                if (eidasLevel.name().equalsIgnoreCase(eidasLevelName)) {
                    return eidasLevel;
                }
            }
            return defaultEidasLevel;
        }
    }

}
