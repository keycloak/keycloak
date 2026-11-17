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
package org.keycloak.protocol.saml.mappers;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

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
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "member");
        config.put(GroupMembershipMapper.SINGLE_GROUP_ATTRIBUTE, "true");
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

    /**
     * Creates a group stub with a name, id and an optional parent group.
     */
    private static GroupModel createGroup(String name, String id, GroupModel parent) {
        Map<String, Object> returns = new HashMap<>();
        returns.put("getName", name);
        returns.put("getId", id);
        if (parent != null) {
            returns.put("getParent", parent);
        }
        return stub(GroupModel.class, returns);
    }

    private static UserSessionModel createUserSessionWithGroups(GroupModel... groups) {
        List<GroupModel> groupList = Arrays.asList(groups);
        Map<String, Object> userReturns = Collections.singletonMap("getGroupsStream",
            (Supplier<Stream<GroupModel>>) () -> groupList.stream());
        UserModel user = stub(UserModel.class, userReturns);
        Map<String, Object> sessionReturns = Collections.singletonMap("getUser", user);
        return stub(UserSessionModel.class, sessionReturns);
    }

    private static List<Object> attributeValues(AttributeStatementType attributeStatement) {
        MatcherAssert.assertThat(attributeStatement.getAttributes().size(), Matchers.equalTo(1));
        return attributeStatement.getAttributes().get(0).getAttribute().getAttributeValue();
    }

    @Test
    public void testTransformAttributeStatementExcludesParentGroupsByDefault() {
        ProtocolMapperModel mappingModel = createMappingModel();
        GroupModel top = createGroup("top", "id-top", null);
        GroupModel level1 = createGroup("level1", "id-level1", top);
        UserSessionModel userSession = createUserSessionWithGroups(level1);

        AttributeStatementType attributeStatement = new AttributeStatementType();
        new GroupMembershipMapper().transformAttributeStatement(attributeStatement, mappingModel, null, userSession, null);

        // without include.parent.groups, only the direct group membership is mapped
        MatcherAssert.assertThat(attributeValues(attributeStatement), Matchers.equalTo(Collections.singletonList("level1")));
    }

    @Test
    public void testTransformAttributeStatementIncludesParentGroupsWhenEnabled() {
        ProtocolMapperModel mappingModel = createMappingModel();
        mappingModel.getConfig().put("include.parent.groups", "true");
        GroupModel top = createGroup("top", "id-top", null);
        GroupModel level1 = createGroup("level1", "id-level1", top);
        UserSessionModel userSession = createUserSessionWithGroups(level1);

        AttributeStatementType attributeStatement = new AttributeStatementType();
        new GroupMembershipMapper().transformAttributeStatement(attributeStatement, mappingModel, null, userSession, null);

        // the parent group is included, ordered from the top-level group down
        MatcherAssert.assertThat(attributeValues(attributeStatement), Matchers.equalTo(Arrays.asList("top", "level1")));
    }

    @Test
    public void testTransformAttributeStatementIncludesParentGroupsWithFullPath() {
        ProtocolMapperModel mappingModel = createMappingModel();
        mappingModel.getConfig().put("full.path", "true");
        mappingModel.getConfig().put("include.parent.groups", "true");
        GroupModel top = createGroup("top", "id-top", null);
        GroupModel level1 = createGroup("level1", "id-level1", top);
        UserSessionModel userSession = createUserSessionWithGroups(level1);

        AttributeStatementType attributeStatement = new AttributeStatementType();
        new GroupMembershipMapper().transformAttributeStatement(attributeStatement, mappingModel, null, userSession, null);

        MatcherAssert.assertThat(attributeValues(attributeStatement), Matchers.equalTo(Arrays.asList("/top", "/top/level1")));
    }

    @Test
    public void testTransformAttributeStatementDeduplicatesWhenMemberOfAncestorAndDescendant() {
        ProtocolMapperModel mappingModel = createMappingModel();
        mappingModel.getConfig().put("include.parent.groups", "true");
        GroupModel top = createGroup("top", "id-top", null);
        GroupModel level1 = createGroup("level1", "id-level1", top);
        // the user is a direct member of both the group and its ancestor
        UserSessionModel userSession = createUserSessionWithGroups(top, level1);

        AttributeStatementType attributeStatement = new AttributeStatementType();
        new GroupMembershipMapper().transformAttributeStatement(attributeStatement, mappingModel, null, userSession, null);

        // the ancestor appears only once
        MatcherAssert.assertThat(attributeValues(attributeStatement), Matchers.equalTo(Arrays.asList("top", "level1")));
    }

    @Test
    public void testTransformAttributeStatementSeparateAttributesPerGroup() {
        ProtocolMapperModel mappingModel = createMappingModel();
        mappingModel.getConfig().put(GroupMembershipMapper.SINGLE_GROUP_ATTRIBUTE, "false");
        mappingModel.getConfig().put("include.parent.groups", "true");
        GroupModel top = createGroup("top", "id-top", null);
        GroupModel level1 = createGroup("level1", "id-level1", top);
        UserSessionModel userSession = createUserSessionWithGroups(level1);

        AttributeStatementType attributeStatement = new AttributeStatementType();
        new GroupMembershipMapper().transformAttributeStatement(attributeStatement, mappingModel, null, userSession, null);

        // one attribute per group, in order from the top-level group down
        MatcherAssert.assertThat(attributeStatement.getAttributes(), Matchers.hasSize(2));
        List<Object> values = new ArrayList<>();
        attributeStatement.getAttributes().forEach(choice -> {
            MatcherAssert.assertThat(choice.getAttribute().getAttributeValue(), Matchers.hasSize(1));
            values.addAll(choice.getAttribute().getAttributeValue());
        });
        MatcherAssert.assertThat(values, Matchers.equalTo(Arrays.asList("top", "level1")));
    }
}
