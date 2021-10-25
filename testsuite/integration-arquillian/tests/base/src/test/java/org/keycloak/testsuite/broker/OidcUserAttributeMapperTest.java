package org.keycloak.testsuite.broker;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class OidcUserAttributeMapperTest extends AbstractUserAttributeMapperTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("attribute-mapper");
        attrMapper1.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.CLAIM, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME)
          .put(UserAttributeMapper.USER_ATTRIBUTE, MAPPED_ATTRIBUTE_NAME)
          .build());

        IdentityProviderMapperRepresentation emailAttrMapper = new IdentityProviderMapperRepresentation();
        emailAttrMapper.setName("attribute-mapper-email");
        emailAttrMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        emailAttrMapper.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.CLAIM, "email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "email")
          .build());

        IdentityProviderMapperRepresentation nestedEmailAttrMapper = new IdentityProviderMapperRepresentation();
        nestedEmailAttrMapper.setName("nested-attribute-mapper-email");
        nestedEmailAttrMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        nestedEmailAttrMapper.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.CLAIM, "nested.email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "nested.email")
          .build());

        IdentityProviderMapperRepresentation dottedEmailAttrMapper = new IdentityProviderMapperRepresentation();
        dottedEmailAttrMapper.setName("dotted-attribute-mapper-email");
        dottedEmailAttrMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        dottedEmailAttrMapper.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.CLAIM, "dotted\\.email")
          .put(UserAttributeMapper.USER_ATTRIBUTE, "dotted.email")
          .build());

        IdentityProviderMapperRepresentation emailVerifiedAttrMapper = new IdentityProviderMapperRepresentation();
        emailVerifiedAttrMapper.setName("emailVerified");
        emailVerifiedAttrMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        emailVerifiedAttrMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put(UserAttributeMapper.CLAIM, "email_verified")
                .put(UserAttributeMapper.USER_ATTRIBUTE, "emailVerified")
                .build());

        return Lists.newArrayList(attrMapper1, emailAttrMapper, nestedEmailAttrMapper, dottedEmailAttrMapper, emailVerifiedAttrMapper);
    }

    @Test
    public void testImportEmailVerified() {
        testValueMappingForImportSyncMode(ImmutableMap.<String, List<String>>builder()
                        .put("emailVerified", ImmutableList.<String>builder().add("true").build())
                        .build(),
                ImmutableMap.<String, List<String>>builder()
                        .put("emailVerified", ImmutableList.<String>builder().add("false").build())
                        .build()
        );
    }

    @Test
    public void testForceEmailVerified() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
                        .put("emailVerified", ImmutableList.<String>builder().add("true").build())
                        .build(),
                ImmutableMap.<String, List<String>>builder()
                        .put("emailVerified", ImmutableList.<String>builder().add("false").build())
                        .build()
        );
    }

}
