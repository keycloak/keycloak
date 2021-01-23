package org.keycloak.testsuite.broker;

import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

public class SamlUserAttributeMapperTest extends AbstractUserAttributeMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation attrMapperEmail = new IdentityProviderMapperRepresentation();
        attrMapperEmail.setName("attribute-mapper-email");
        attrMapperEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapperEmail.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "email")
          .build());

        IdentityProviderMapperRepresentation attrMapperNestedEmail = new IdentityProviderMapperRepresentation();
        attrMapperNestedEmail.setName("nested-attribute-mapper-email");
        attrMapperNestedEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapperNestedEmail.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.ATTRIBUTE_NAME, "nested.email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "nested.email")
          .build());

        IdentityProviderMapperRepresentation attrMapperDottedEmail = new IdentityProviderMapperRepresentation();
        attrMapperDottedEmail.setName("dotted-attribute-mapper-email");
        attrMapperDottedEmail.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapperDottedEmail.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.ATTRIBUTE_NAME, "dotted.email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "dotted.email")
          .build());

        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("attribute-mapper");
        attrMapper1.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.ATTRIBUTE_NAME, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME)
          .put(UserAttributeMapper.USER_ATTRIBUTE, MAPPED_ATTRIBUTE_NAME)
          .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("attribute-mapper-friendly");
        attrMapper2.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, ATTRIBUTE_TO_MAP_FRIENDLY_NAME)
          .put(UserAttributeMapper.USER_ATTRIBUTE, MAPPED_ATTRIBUTE_FRIENDLY_NAME)
          .build());

        return Lists.newArrayList(attrMapperEmail, attrMapper1, attrMapper2, attrMapperDottedEmail, attrMapperNestedEmail);
    }

}
