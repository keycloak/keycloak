package org.keycloak.testsuite.broker;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.broker.oidc.mappers.PrefixedUserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AccountHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class OidcEmptyPrefixedUserAttributeMapperTest extends AbstractUserAttributeMapperTest {

    protected static final String MAPPED_ATTRIBUTE_NAME = "mapped-user-attribute";
    protected static final String MAPPED_ATTRIBUTE_FRIENDLY_NAME = "mapped-user-attribute-friendly";

    private static final String PREFIX = "";
    private static final Set<String> PROTECTED_NAMES = ImmutableSet.<String>builder().add("email").add("lastName").add("firstName").build();
    private static final Map<String, String> ATTRIBUTE_NAME_TRANSLATION = ImmutableMap.<String, String>builder()
      .put("dotted.email", "dotted.email")
      .put("nested.email", "nested.email")
      .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, MAPPED_ATTRIBUTE_FRIENDLY_NAME)
      .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, MAPPED_ATTRIBUTE_NAME)
      .build();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode) {
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

        IdentityProviderMapperRepresentation emptyPrefixAttrMapper = new IdentityProviderMapperRepresentation();
        emptyPrefixAttrMapper.setName("empty-prefixed-attribute-mapper");
        emptyPrefixAttrMapper.setIdentityProviderMapper(PrefixedUserAttributeMapper.PROVIDER_ID);
        emptyPrefixAttrMapper.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(PrefixedUserAttributeMapper.CLAIM, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME)
          .put(PrefixedUserAttributeMapper.USER_ATTRIBUTE, MAPPED_ATTRIBUTE_NAME)
          .put(PrefixedUserAttributeMapper.ATTRIBUTE_PREFFIX, PREFIX)
          .build());

        return Lists.newArrayList(emailAttrMapper, nestedEmailAttrMapper, dottedEmailAttrMapper, emptyPrefixAttrMapper);
    }

}
