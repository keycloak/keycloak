package org.keycloak.infinispan.module.configuration.global;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.keycloak.infinispan.module.certificates.JGroupsCertificateHolder;
import org.keycloak.models.KeycloakSessionFactory;

public class KeycloakConfigurationBuilder implements Builder<KeycloakConfiguration> {

    private final AttributeSet attributes;

    public KeycloakConfigurationBuilder(GlobalConfigurationBuilder unused) {
        attributes = KeycloakConfiguration.attributeSet();
    }

    @Override
    public KeycloakConfiguration create() {
        return new KeycloakConfiguration(attributes.protect());
    }

    @Override
    public Builder<?> read(KeycloakConfiguration template, Combine combine) {
        attributes.read(template.attributes(), combine);
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return attributes;
    }

    @Override
    public void validate() {

    }

    public KeycloakConfigurationBuilder setKeycloakSessionFactory(KeycloakSessionFactory keycloakSessionFactory) {
        attributes.attribute(KeycloakConfiguration.KEYCLOAK_SESSION_FACTORY).set(keycloakSessionFactory);
        return this;
    }

    public KeycloakConfigurationBuilder setJGroupCertificateHolder(JGroupsCertificateHolder jGroupsCertificateHolder) {
        attributes.attribute(KeycloakConfiguration.JGROUPS_CERTIFICATE_HOLDER).set(jGroupsCertificateHolder);
        return this;
    }

    public KeycloakConfigurationBuilder setJGroupsCertificateRotation(int days) {
        attributes.attribute(KeycloakConfiguration.JGROUPS_CERTIFICATE_ROTATION).set(days);
        return this;
    }
}
