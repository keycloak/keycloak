/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
 */
package org.keycloak.protocol.oidc.mappers;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.IDToken;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Unit tests for {@link GroupMembershipMapper}.
 */
public class GroupMembershipMapperTest {

    private static ProtocolMapperModel createMappingModel() {
        ProtocolMapperModel mappingModel = new ProtocolMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
        config.put("full.path", "false");
        mappingModel.setName("group membership");
        mappingModel.setConfig(config);
        return mappingModel;
    }

    /**
     * Creates a stub of the given interface that returns the supplied values for the matching method names,
     * and neutral defaults (null, false, 0) for everything else. Supplier values are invoked per call so
     * that one-shot objects like streams can be produced fresh on every invocation.
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Map<String, Object> methodReturns) {
        return (T) Proxy.newProxyInstance(GroupMembershipMapperTest.class.getClassLoader(), new Class<?>[] {type},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "Stub of " + type.getSimpleName();
                }
                Object returnValue = methodReturns.get(method.getName());
                if (returnValue instanceof Supplier) {
                    returnValue = ((Supplier<?>) returnValue).get();
                }
                if (returnValue != null) {
                    return returnValue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return Boolean.FALSE;
                }
                if (returnType == int.class) {
                    return 0;
                }
                if (returnType == long.class) {
                    return 0L;
                }
                return null;
            });
    }

    private static UserSessionModel createUserSessionWithGroups(String... groupNames) {
        List<String> names = Arrays.asList(groupNames);
        Map<String, Object> userReturns = Collections.singletonMap("getGroupsStream",
            (Supplier<Stream<GroupModel>>) () -> names.stream()
                .map(name -> stub(GroupModel.class, Collections.singletonMap("getName", name))));
        UserModel user = stub(UserModel.class, userReturns);
        Map<String, Object> sessionReturns = Collections.singletonMap("getUser", user);
        return stub(UserSessionModel.class, sessionReturns);
    }

    @Test
    public void testSetClaimMapsGroupsWithoutMutatingTheMappingModel() {
        ProtocolMapperModel mappingModel = createMappingModel();
        UserSessionModel userSession = createUserSessionWithGroups("admins", "developers");
        GroupMembershipMapper mapper = new GroupMembershipMapper();

        IDToken token = new IDToken();
        mapper.setClaim(token, mappingModel, userSession);

        MatcherAssert.assertThat(token.getOtherClaims().get("groups"), Matchers.equalTo(Arrays.asList("admins", "developers")));
        // the mapper forces multivalued on a copy of the model and must leave the shared, cached config untouched
        MatcherAssert.assertThat(mappingModel.getConfig(), Matchers.not(Matchers.hasKey("multivalued")));
    }

    @Test
    public void testSetClaimCanBeRepeatedlyInvokedWithTheSameMappingModel() {
        ProtocolMapperModel mappingModel = createMappingModel();
        UserSessionModel userSession = createUserSessionWithGroups("admins");
        GroupMembershipMapper mapper = new GroupMembershipMapper();

        IDToken firstToken = new IDToken();
        mapper.setClaim(firstToken, mappingModel, userSession);

        // simulate the cached mapping model being reused for the next token issuance
        IDToken secondToken = new IDToken();
        mapper.setClaim(secondToken, mappingModel, userSession);

        MatcherAssert.assertThat(secondToken.getOtherClaims().get("groups"), Matchers.equalTo(Collections.singletonList("admins")));
        MatcherAssert.assertThat(mappingModel.getConfig(), Matchers.not(Matchers.hasKey("multivalued")));
    }
}
