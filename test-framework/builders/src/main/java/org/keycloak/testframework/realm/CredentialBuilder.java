package org.keycloak.testframework.realm;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;

public class CredentialBuilder extends Builder<CredentialRepresentation> {

    private CredentialBuilder(CredentialRepresentation rep) {
        super(rep);
    }

    public static CredentialBuilder create() {
        return new CredentialBuilder(new CredentialRepresentation());
    }

    public static CredentialBuilder password(String password) {
        return create().type(CredentialRepresentation.PASSWORD).value(password);
    }

    public static CredentialBuilder totp(String totpSecret) {
        return update(ModelToRepresentation.toRepresentation(OTPCredentialModel.createTOTP(totpSecret, 6, 30, HmacOTP.HMAC_SHA1)));
    }

    public static CredentialBuilder hotp(String hotpSecret) {
        return update(ModelToRepresentation.toRepresentation(OTPCredentialModel.createHOTP(hotpSecret, 6, 0, HmacOTP.HMAC_SHA1)));
    }

    public static CredentialBuilder update(CredentialRepresentation rep) {
        return new CredentialBuilder(rep);
    }

    public CredentialBuilder type(String type) {
        rep.setType(type);
        return this;
    }

    public CredentialBuilder value(String value) {
        rep.setValue(value);
        return this;
    }

    public CredentialBuilder secretData(String secretData) {
        rep.setSecretData(secretData);
        return this;
    }

}
