package org.keycloak.infinispan.module.configuration.global;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.keycloak.infinispan.module.certificates.JGroupsCertificateHolder;
import org.keycloak.models.KeycloakSessionFactory;

@BuiltBy(KeycloakConfigurationBuilder.class)
public class KeycloakConfiguration {

    static final AttributeDefinition<KeycloakSessionFactory> KEYCLOAK_SESSION_FACTORY = AttributeDefinition.builder("keycloak-session-factory", null, KeycloakSessionFactory.class)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();
    static final AttributeDefinition<JGroupsCertificateHolder> JGROUPS_CERTIFICATE_HOLDER = AttributeDefinition.builder("jgroups-certificate-holder", null, JGroupsCertificateHolder.class)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();
    static final AttributeDefinition<Integer> JGROUPS_CERTIFICATE_ROTATION = AttributeDefinition.builder("jgroups-certificate-rotation", 30, Integer.class)
            .global(true)
            .autoPersist(false)
            .immutable()
            .build();

    private final AttributeSet attributes;

    static AttributeSet attributeSet() {
        return new AttributeSet(KeycloakConfiguration.class, KEYCLOAK_SESSION_FACTORY, JGROUPS_CERTIFICATE_HOLDER, JGROUPS_CERTIFICATE_ROTATION);
    }

    KeycloakConfiguration(AttributeSet attributes) {
        this.attributes = attributes;
    }

    AttributeSet attributes() {
        return attributes;
    }

    public KeycloakSessionFactory keycloakSessionFactory() {
        return attributes.attribute(KEYCLOAK_SESSION_FACTORY).get();
    }

    public JGroupsCertificateHolder jGroupsCertificateHolder() {
        return attributes.attribute(JGROUPS_CERTIFICATE_HOLDER).get();
    }

    public int jgroupsCertificateRotation() {
        return attributes.attribute(JGROUPS_CERTIFICATE_ROTATION).get();
    }
}
