/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.cluster;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.util.RoleBuilder;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.Before;

import static org.keycloak.common.util.reflections.Reflections.resolveListType;
import static org.keycloak.common.util.reflections.Reflections.setAccessible;
import static org.keycloak.common.util.reflections.Reflections.unsetAccessible;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class RoleInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<RoleRepresentation, RoleResource> {

    @Before
    public void setExcludedComparisonFields() {
        excludedComparisonFields.add("composites");
    }

    @Override
    protected RoleRepresentation createTestEntityRepresentation() {
        RoleRepresentation composite1 = createEntityOnCurrentFailNode(RoleBuilder.create()
                .name("composite_role_" + RandomStringUtils.randomAlphabetic(5))
                .build());
        RoleRepresentation composite2 = createEntityOnCurrentFailNode(RoleBuilder.create()
                .name("composite_role_" + RandomStringUtils.randomAlphabetic(5))
                .build());

        RoleRepresentation role = new RoleRepresentation();
        role.setName("role_" + RandomStringUtils.randomAlphabetic(5));
        role.setDescription("description of "+role.getName());

        role.setComposites(new RoleRepresentation.Composites());
        role.getComposites().setRealm(new HashSet<>());
        role.getComposites().getRealm().add(composite1.getName());
        role.getComposites().getRealm().add(composite2.getName());

        return role;
    }

    protected RolesResource roles(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).roles();
    }

    protected RoleByIdResource roleById(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).rolesById();
    }

    @Override
    protected RoleResource entityResource(RoleRepresentation role, ContainerInfo node) {
        return entityResource(role.getName(), node);
    }

    @Override
    protected RoleResource entityResource(String name, ContainerInfo node) {
        return roles(node).get(name);
    }

    @Override
    protected RoleRepresentation createEntity(RoleRepresentation role, ContainerInfo node) {
        roles(node).create(role);
        if (role.getComposites() != null && role.getComposites().getRealm() != null) {
            List<RoleRepresentation> composites = role.getComposites().getRealm().stream()
                    .map(realmRoleName -> roles(node).get(realmRoleName).toRepresentation())
                    .collect(Collectors.toList());
            roleById(node).addComposites(readEntity(role, node).getId(), composites);
        }
        return readEntity(role, node);
    }

    @Override
    protected RoleRepresentation readEntity(RoleRepresentation role, ContainerInfo node) {
        RoleRepresentation u = null;
        try {
            u = entityResource(role, node).toRepresentation();
            if (u.isComposite()) {
                u.setComposites(new RoleRepresentation.Composites());
                u.getComposites().setRealm(new HashSet<>());
                for (RoleRepresentation roleComposite : roleById(node).getRealmRoleComposites(u.getId())) {
                    u.getComposites().getRealm().add(roleComposite.getName());
                }
            }
        } catch (NotFoundException nfe) {
            // expected when role doesn't exist
        }
        return u;
    }

    @Override
    protected RoleRepresentation updateEntity(RoleRepresentation role, ContainerInfo node) {
        return updateEntity(role.getName(), role, node);
    }

    private RoleRepresentation updateEntity(String roleName, RoleRepresentation role, ContainerInfo node) {
        entityResource(roleName, node).update(role);
        return readEntity(role, node);
    }

    @Override
    protected void deleteEntity(RoleRepresentation role, ContainerInfo node) {
        entityResource(role, node).remove();
        assertNull(readEntity(role, node));

        //removing remaining composite role
        roles(node).deleteRole(role.getComposites().getRealm().stream().findFirst().get());
    }

    @Override
    protected RoleRepresentation testEntityUpdates(RoleRepresentation role, boolean backendFailover) {

        // description
        role.setDescription(role.getDescription()+"_- updated");
        role = updateEntityOnCurrentFailNode(role, "description");
        verifyEntityUpdateDuringFailover(role, backendFailover);

        //composite role
        log.info("Removing one of the composite roles on " + getCurrentFailNode());
        roles(getCurrentFailNode()).deleteRole(role.getComposites().getRealm().stream().findFirst().get());
        role = readEntity(role, getCurrentFailNode());
        verifyEntityUpdateDuringFailover(role, backendFailover);

        return role;
    }

    @Override
    protected void assertEntityOnSurvivorNodesEqualsTo(RoleRepresentation testEntityOnFailNode) {
        super.assertEntityOnSurvivorNodesEqualsTo(testEntityOnFailNode);

        //composites
        boolean entityDiffers = false;
        for (ContainerInfo survivorNode : getCurrentSurvivorNodes()) {
            log.debug(String.format("Attempt to verify %s on survivor %s (%s)", getEntityType(testEntityOnFailNode), survivorNode, survivorNode.getContextRoot()));
            RoleRepresentation testEntityOnSurvivorNode = readEntity(testEntityOnFailNode, survivorNode);

            if (EqualsBuilder.reflectionEquals(
                    sortFieldsComposites(testEntityOnSurvivorNode.getComposites()), 
                    sortFieldsComposites(testEntityOnFailNode.getComposites()))) {
                log.info(String.format("Verification of %s on survivor %s PASSED", getEntityType(testEntityOnFailNode), survivorNode));
            } else {
                entityDiffers = true;
                log.error(String.format("Verification of %s on survivor %s FAILED", getEntityType(testEntityOnFailNode), survivorNode));
                String tf = ReflectionToStringBuilder.reflectionToString(testEntityOnFailNode.getComposites(), ToStringStyle.SHORT_PREFIX_STYLE);
                String ts = ReflectionToStringBuilder.reflectionToString(testEntityOnSurvivorNode.getComposites(), ToStringStyle.SHORT_PREFIX_STYLE);
                log.error(String.format(
                        "\nEntity on fail node: \n%s\n"
                        + "\nEntity on survivor node: \n%s\n"
                        + "\nDifference: \n%s\n",
                        tf, ts, StringUtils.difference(tf, ts)));
            }
        }
        assertFalse(entityDiffers);
    }

    private RoleRepresentation.Composites sortFieldsComposites(RoleRepresentation.Composites composites) {
        for (Field field : composites.getClass().getDeclaredFields()) {
            try {
                Class<?> type = resolveListType(field, composites);

                if (type != null && Comparable.class.isAssignableFrom(type)) {
                    setAccessible(field);
                    Object value = field.get(composites);

                    if (value != null) {
                        Collections.sort((List) value);
                    }
                }
            } catch (IllegalAccessException cause) {
                throw new RuntimeException("Failed to sort field [" + field + "]", cause);
            } finally {
                unsetAccessible(field);
            }
        }

        return composites;
    }
}
