package org.keycloak.testsuite.broker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class OidcPrefixedUserAttributeMapperTest extends AbstractUserAttributeMapperTest {

    protected static final String MAPPED_ATTRIBUTE_NAME = "mapped-user-attribute";
    protected static final String MAPPED_ATTRIBUTE_FRIENDLY_NAME = "mapped-user-attribute-friendly";

    private static final String PREFIX = "prefix_";
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
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("prefixed-attribute-mapper");
        attrMapper1.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
          .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
          .put(UserAttributeMapper.CLAIM, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME)
          .put(UserAttributeMapper.USER_ATTRIBUTE, MAPPED_ATTRIBUTE_NAME)
          .put(UserAttributeMapper.ATTRIBUTE_PREFFIX, PREFIX)
          .build());

        return Lists.newArrayList(attrMapper1);
    }

    private List<String> toPrefixedList(Object value, String prefix) {
        List<Object> values = (value.getClass().isArray())
                ? Arrays.asList((Object[]) value)
                : Collections.singletonList(value);

        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> prefix.concat(item.toString()))
                .collect(Collectors.toList());
    }

    @Override
    protected void assertUserAttributes(Map<String, List<String>> attrs, UserRepresentation userRep) {
        Set<String> mappedAttrNames = attrs.entrySet().stream()
          .filter(me -> me.getValue() != null && ! me.getValue().isEmpty())
          .map(me -> me.getKey())
          .filter(a -> ! PROTECTED_NAMES.contains(a))
          .map(ATTRIBUTE_NAME_TRANSLATION::get)
          .collect(Collectors.toSet());

        if (mappedAttrNames.isEmpty()) {
            assertThat("No attributes are expected to be present", userRep.getAttributes(), nullValue());
        } else if (attrs.containsKey("email")) {
            assertThat(userRep.getEmail(), equalTo(attrs.get("email").get(0)));
        } else {
            assertThat(userRep.getAttributes(), notNullValue());
            assertThat(userRep.getAttributes().keySet(), equalTo(mappedAttrNames));
            for (Map.Entry<String, List<String>> me : attrs.entrySet()) {
                String mappedAttrName = ATTRIBUTE_NAME_TRANSLATION.get(me.getKey());
                if (mappedAttrNames.contains(mappedAttrName)) {
                    log.info(userRep.getAttributes());
                    assertThat(userRep.getAttributes().get(mappedAttrName), containsInAnyOrder(toPrefixedList(me.getValue().toArray(), PREFIX).toArray()));
                }
            }
        }

    }

}