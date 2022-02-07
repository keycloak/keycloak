/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.forms.login.freemarker.model;

import com.webauthn4j.data.AuthenticatorTransport;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.theme.DateTimeFormatterUtil;
import org.keycloak.utils.StringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class WebAuthnAuthenticatorsBean {

    private final List<WebAuthnAuthenticatorBean> authenticators;

    public WebAuthnAuthenticatorsBean(KeycloakSession session, RealmModel realm, UserModel user, String credentialType) {
        // should consider multiple credentials in the future, but only single credential supported now.
        this.authenticators = session.userCredentialManager().getStoredCredentialsByTypeStream(realm, user, credentialType)
                .map(WebAuthnCredentialModel::createFromCredentialModel)
                .map(webAuthnCredential -> {
                    String credentialId = Base64Url.encodeBase64ToBase64Url(webAuthnCredential.getWebAuthnCredentialData().getCredentialId());
                    String label = (webAuthnCredential.getUserLabel() == null || webAuthnCredential.getUserLabel().isEmpty()) ? "label missing" : webAuthnCredential.getUserLabel();
                    String createdAt = DateTimeFormatterUtil.getDateTimeFromMillis(webAuthnCredential.getCreatedDate(), session.getContext().resolveLocale(user));
                    final Set<String> transports = webAuthnCredential.getWebAuthnCredentialData().getTransports();

                    return new WebAuthnAuthenticatorBean(credentialId, label, createdAt, transports);
                }).collect(Collectors.toList());
    }

    public List<WebAuthnAuthenticatorBean> getAuthenticators() {
        return authenticators;
    }

    public static class WebAuthnAuthenticatorBean {
        public static final String DEFAULT_ICON = "kcWebAuthnDefaultIcon";

        private final String credentialId;
        private final String label;
        private final String createdAt;
        private final TransportsBean transports;

        public WebAuthnAuthenticatorBean(String credentialId, String label, String createdAt, Set<String> transports) {
            this.credentialId = credentialId;
            this.label = label;
            this.createdAt = createdAt;
            this.transports = TransportsBean.convertFromSet(transports);
        }

        public String getCredentialId() {
            return this.credentialId;
        }

        public String getLabel() {
            return this.label;
        }

        public String getCreatedAt() {
            return this.createdAt;
        }

        public TransportsBean getTransports() {
            return transports;
        }

        public static class TransportsBean {
            private final Set<String> displayNameProperties;
            private final String iconClass;

            public TransportsBean(Set<String> displayNameProperties, String iconClass) {
                this.displayNameProperties = displayNameProperties;
                this.iconClass = iconClass;
            }

            public TransportsBean(String displayNameProperty, String iconClass) {
                this(Collections.singleton(displayNameProperty), iconClass);
            }

            public TransportsBean(Transport transport) {
                this(transport.getDisplayNameProperty(), transport.getIconClass());
            }

            public Set<String> getDisplayNameProperties() {
                return displayNameProperties;
            }

            public String getIconClass() {
                return iconClass;
            }

            /**
             * Converts set of available transport media to TransportsBean
             *
             * @param transports set of available transport media
             * @return TransportBean
             */
            public static TransportsBean convertFromSet(Set<String> transports) {
                if (CollectionUtil.isEmpty(transports)) {
                    return new TransportsBean(Transport.UNKNOWN);
                }

                final Set<Transport> trans = transports.stream()
                        .filter(Objects::nonNull)
                        .map(Transport::getByMapperName)
                        .collect(Collectors.toSet());

                if (trans.size() <= 1) {
                    final Transport transport = trans.stream()
                            .findFirst()
                            .orElse(Transport.UNKNOWN);

                    return new TransportsBean(transport);
                } else {
                    final Set<String> displayNameProperties = trans.stream()
                            .map(Transport::getDisplayNameProperty)
                            .collect(Collectors.toSet());

                    return new TransportsBean(displayNameProperties, DEFAULT_ICON);
                }
            }

            protected enum Transport {
                USB("usb", AuthenticatorTransport.USB.getValue(), "kcWebAuthnUSB"),
                NFC("nfc", AuthenticatorTransport.NFC.getValue(), "kcWebAuthnNFC"),
                BLE("bluetooth", AuthenticatorTransport.BLE.getValue(), "kcWebAuthnBLE"),
                INTERNAL("internal", AuthenticatorTransport.INTERNAL.getValue(), "kcWebAuthnInternal"),
                UNKNOWN("unknown", null, DEFAULT_ICON);

                private final String displayNameProperty;
                private final String mapperName;
                private final String iconClass;

                /**
                 * @param displayNameProperty Message property - defined in messages_xx.properties
                 * @param mapperName used for mapping transport media name
                 * @param iconClass icon class for particular transport media - defined in theme.properties
                 */
                Transport(String displayNameProperty, String mapperName, String iconClass) {
                    this.displayNameProperty = displayNameProperty;
                    this.mapperName = mapperName;
                    this.iconClass = iconClass;
                }

                public String getDisplayNameProperty() {
                    return displayNameProperty;
                }

                public String getMapperName() {
                    return mapperName;
                }

                public String getIconClass() {
                    return iconClass;
                }

                public static Transport getByDisplayNameProperty(String property) {
                    return Arrays.stream(Transport.values())
                            .filter(f -> f.getDisplayNameProperty().equals(property))
                            .findFirst()
                            .orElse(UNKNOWN);
                }

                public static Transport getByMapperName(String mapperName) {
                    if (StringUtil.isBlank(mapperName)) return UNKNOWN;

                    return Arrays.stream(Transport.values())
                            .filter(f -> Objects.nonNull(f.getMapperName()))
                            .filter(f -> f.getMapperName().equals(mapperName))
                            .findFirst()
                            .orElse(UNKNOWN);
                }
            }
        }
    }
}
