package org.keycloak.spi.authentication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthProviderConstants {

    // Model is default provider. See AuthenticationProviderModel.DEFAULT_PROVIDER
    public static final String PROVIDER_NAME_MODEL = "model";
    public static final String PROVIDER_NAME_EXTERNAL_MODEL = "externalModel";
    public static final String PROVIDER_NAME_PICKETLINK = "picketlink";

    // Used in external-model provider
    public static final String EXTERNAL_REALM_ID = "externalRealmId";
}
