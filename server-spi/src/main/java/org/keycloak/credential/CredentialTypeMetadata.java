/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.credential;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialTypeMetadata {

    public static final String DEFAULT_ICON_CSS_CLASS = "kcAuthenticatorDefaultClass";

    private String type;

    private String displayName;

    private String helpText;

    private String iconCssClass = DEFAULT_ICON_CSS_CLASS;

    private String createAction;

    private String updateAction;

    private Boolean removeable;

    private Category category;


    public enum Category {
        PASSWORD("password"),
        TWO_FACTOR("two-factor"),
        PASSWORDLESS("passwordless");

        private String categoryName;

        Category(String categoryName) {
            this.categoryName = categoryName;
        }

        @Override
        public String toString() {
            return categoryName;
        }
    }


    private CredentialTypeMetadata() {
    }


    // GETTERS

    /**
     * @return credential type like for example "password", "otp" or "webauthn"
     */
    public String getType() {
        return type;
    }

   /**
     * @return the label, which will be shown to the end user on various screens, like login screen with available authentication mechanisms.
     * This label will reference this particular authenticator type.
     * It should be clear to end users. For example, implementations can return "Authenticator Application" for OTP or "Security Key" for WebAuthn.
     *
     * Alternatively, this method can return a message key, so that it is possible to localize it for various languages.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the text, which will be shown to the user on various screens, like login screen with available authentication mechanisms.
     * This text will reference this particular authenticator type.
     * For example for OTP, the returned text could be "Enter a verification code from authenticator application" .
     *
     * Alternatively, this method can return a message key, so that it is possible to localize it for various languages.
     */
    public String getHelpText() {
        return helpText;
    }

    /**
     * Return the icon CSS, which can be used to display icon, which represents this particular authenticator.
     *
     * The icon will be displayed on various places. For example the "Select authenticator" screen during login, where user can select from
     * various authentication mechanisms for two-factor or passwordless authentication.
     *
     * The returned value can be either:
     * - Key of the property, which will reference the actual CSS in the themes.properties file. For example if you return "kcAuthenticatorWebAuthnClass"
     *   from this method, then your themes.properties should have the property like for example "kcAuthenticatorWebAuthnClass=fa fa-key list-view-pf-icon-lg" .
     *   This would mean that "fa fa-key list-view-pf-icon-lg" will be the actual CSS used.
     * - the icon CSS class directly. For example you can return "fa fa-key list-view-pf-icon-lg" directly for the above example with WebAuthn.
     *   This alternative is fine just if your authenticator can use same CSS class for all the themes.
     *
     * If you don't expect your authenticator to need icon (for example it will never be shown in the "select authenticator" screen), then
     * it is fine to keep the default value.
     */
    public String getIconCssClass() {
        return iconCssClass;
    }

    public String getCreateAction() {
        return createAction;
    }

    public String getUpdateAction() {
        return updateAction;
    }

    public boolean isRemoveable() {
        return removeable;
    }

    public Category getCategory() {
        return category;
    }

    public static CredentialTypeMetadataBuilder builder() {
        return new CredentialTypeMetadataBuilder();
    }

    // BUILDER

    public static class CredentialTypeMetadataBuilder {

        private CredentialTypeMetadata instance = new CredentialTypeMetadata();

        public CredentialTypeMetadataBuilder type(String type) {
            instance.type = type;
            return this;
        }

        public CredentialTypeMetadataBuilder displayName(String displayName) {
            instance.displayName = displayName;
            return this;
        }

        public CredentialTypeMetadataBuilder helpText(String helpText) {
            instance.helpText = helpText;
            return this;
        }

        public CredentialTypeMetadataBuilder iconCssClass(String iconCssClass) {
            instance.iconCssClass = iconCssClass;
            return this;
        }

        public CredentialTypeMetadataBuilder createAction(String createAction) {
            instance.createAction = createAction;
            return this;
        }

        public CredentialTypeMetadataBuilder updateAction(String updateAction) {
            instance.updateAction = updateAction;
            return this;
        }

        public CredentialTypeMetadataBuilder removeable(boolean removeable) {
            instance.removeable = removeable;
            return this;
        }

        public CredentialTypeMetadataBuilder category(Category category) {
            instance.category = category;
            return this;
        }

        /**
         * This will validate metadata and return them
         *
         * @return metadata
         */
        public CredentialTypeMetadata build() {
            assertNotNull(instance.type, "type");
            assertNotNull(instance.displayName, "displayName");
            assertNotNull(instance.helpText, "helpText");
            assertNotNull(instance.iconCssClass, "iconCssClass");
            assertNotNull(instance.removeable, "removeable");
            assertNotNull(instance.category, "category");

            // Assume credential can't have both createAction and updateAction. Exactly 1 of them should be non-null
            if (instance.createAction == null && instance.updateAction == null) {
                throw new IllegalStateException("Both createAction and updateAction are null when building CredentialTypeMetadata for the credential type '" + instance.type);
            }
            if (instance.createAction != null && instance.updateAction != null) {
                throw new IllegalStateException("Both createAction and updateAction are not null when building CredentialTypeMetadata for the credential type '" + instance.type);
            }

            return instance;
        }

        private void assertNotNull(Object input, String fieldName) {
            if (input == null) {
                throw new IllegalStateException("Field '" + fieldName + "' is null when building CredentialTypeMetadata for the credential type '" + instance.type);
            }
        }

    }



}
