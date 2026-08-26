package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Helper class for retrieving WebAuthn data
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class WebAuthnRealmData {

    private final RealmRepresentation realm;
    private final boolean isPasswordless;

    public WebAuthnRealmData(RealmRepresentation realm, boolean isPasswordless) {
        this.realm = Objects.requireNonNull(realm, "RealmRepresentation must not be null");
        this.isPasswordless = isPasswordless;
    }

    public String getRpEntityName() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRpEntityName() : realm.getWebAuthnPolicyRpEntityName();
    }

    public List<String> getSignatureAlgorithms() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessSignatureAlgorithms() : realm.getWebAuthnPolicySignatureAlgorithms();
    }

    public String getRpId() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRpId() : realm.getWebAuthnPolicyRpId();
    }

    public String getAttestationConveyancePreference() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAttestationConveyancePreference() : realm.getWebAuthnPolicyAttestationConveyancePreference();
    }

    public String getAuthenticatorAttachment() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAuthenticatorAttachment() : realm.getWebAuthnPolicyAuthenticatorAttachment();
    }

    /**
     * @deprecated Use {@link #getResidentKey()} instead.
     */
    @Deprecated
    public String getRequireResidentKey() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessRequireResidentKey() : realm.getWebAuthnPolicyRequireResidentKey();
    }

    public String getResidentKey() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessResidentKey() : realm.getWebAuthnPolicyResidentKey();
    }

    public String getUserVerificationRequirement() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessUserVerificationRequirement() : realm.getWebAuthnPolicyUserVerificationRequirement();
    }

    public Integer getCreateTimeout() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessCreateTimeout() : realm.getWebAuthnPolicyCreateTimeout();
    }

    public Boolean isAvoidSameAuthenticatorRegister() {
        return isPasswordless ? realm.isWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister() : realm.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
    }

    public List<String> getAcceptableAaguids() {
        return isPasswordless ? realm.getWebAuthnPolicyPasswordlessAcceptableAaguids() : realm.getWebAuthnPolicyAcceptableAaguids();
    }

    public RealmRepresentation getRealm() {
        return realm;
    }

    public Builder builder() {
        return new Builder(realm, isPasswordless);
    }

    public static class Builder {
        private final RealmRepresentation realm;
        private final boolean isPasswordless;

        public Builder(RealmRepresentation realm, boolean isPasswordless) {
            this.realm = realm;
            this.isPasswordless = isPasswordless;
        }

        public Builder rpEntityName(String entityName) {
            setProperty(entityName, realm::setWebAuthnPolicyRpEntityName, realm::setWebAuthnPolicyPasswordlessRpEntityName);
            return this;
        }

        public Builder signatureAlgorithms(List<String> list) {
            setProperty(list, realm::setWebAuthnPolicySignatureAlgorithms, realm::setWebAuthnPolicyPasswordlessSignatureAlgorithms);
            return this;
        }

        public Builder rpId(String rpId) {
            setProperty(rpId, realm::setWebAuthnPolicyRpId, realm::setWebAuthnPolicyPasswordlessRpId);
            return this;
        }

        public Builder attestationConveyancePreference(String preference) {
            setProperty(preference, realm::setWebAuthnPolicyAttestationConveyancePreference, realm::setWebAuthnPolicyPasswordlessAttestationConveyancePreference);
            return this;
        }

        public Builder authenticatorAttachment(String attachment) {
            setProperty(attachment, realm::setWebAuthnPolicyAuthenticatorAttachment, realm::setWebAuthnPolicyPasswordlessAuthenticatorAttachment);
            return this;
        }

        /**
         * @deprecated Use {@link #residentKey(String)} instead.
         */
        @Deprecated
        public Builder requireResidentKey(String requirement) {
            setProperty(requirement, realm::setWebAuthnPolicyRequireResidentKey, realm::setWebAuthnPolicyPasswordlessRequireResidentKey);
            return this;
        }

        public Builder residentKey(String requirement) {
            setProperty(requirement, realm::setWebAuthnPolicyResidentKey, realm::setWebAuthnPolicyPasswordlessResidentKey);
            return this;
        }

        public Builder userVerificationRequirement(String requirement) {
            setProperty(requirement, realm::setWebAuthnPolicyUserVerificationRequirement, realm::setWebAuthnPolicyPasswordlessUserVerificationRequirement);
            return this;
        }

        public Builder timeout(Integer timeout) {
            setProperty(timeout, realm::setWebAuthnPolicyCreateTimeout, realm::setWebAuthnPolicyPasswordlessCreateTimeout);
            return this;
        }

        public Builder avoidSameAuthenticatorRegister(Boolean state) {
            setProperty(state, realm::setWebAuthnPolicyAvoidSameAuthenticatorRegister, realm::setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister);
            return this;
        }

        public Builder acceptableAaguids(List<String> aaguids) {
            setProperty(aaguids, realm::setWebAuthnPolicyAcceptableAaguids, realm::setWebAuthnPolicyPasswordlessAcceptableAaguids);
            return this;
        }

        public RealmRepresentation build() {
            return realm;
        }

        private <T> void setProperty(T value, Consumer<T> webauthnSetter, Consumer<T> passwordlessSetter) {
            if (isPasswordless) {
                passwordlessSetter.accept(value);
            } else {
                webauthnSetter.accept(value);
            }
        }
    }
}
