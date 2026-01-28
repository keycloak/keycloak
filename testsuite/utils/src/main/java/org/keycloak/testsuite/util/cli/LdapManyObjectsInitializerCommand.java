/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;

/**
 * The command requires that:
 * - Realm has 1 LDAP storage provider defined
 * - The LDAP provider has user-attribute-mapper named "streetMapper", which has both "User Model Attribute" and "LDAP Attribute" configured to "street"
 * - The LDAP provider has group-mapper named "groupsMapper", with:
 * -- "LDAP Groups DN" pointing to same DN, like this command <groups-dn> .
 * -- It's supposed to use "User Roles Retrieve Strategy" - "GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE"
 * -- It's supposed to use "Member-Of LDAP Attribute" - "street"
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LdapManyObjectsInitializerCommand extends AbstractCommand {

    @Override
    public String getName() {
        return "createLdapObjects";
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <realm-name> <groups-dn> <start-offset-users> <count-users> <start-offset-groups> <count-groups> .\nSee javadoc of class LdapManyObjectsInitializerCommand for additional details.";
    }

    @Override
    protected void doRunCommand(KeycloakSession session) {
        String realmName = getArg(0);
        String groupsDn = getArg(1);
        int startOffsetUsers = getIntArg(2);
        int countUsers = getIntArg(3);
        int batchCount = 100;
        int startOffsetGroups = getIntArg(4);
        int countGroups = getIntArg(5);

        RealmModel realm = session.realms().getRealmByName(realmName);
        List<ComponentModel> components = realm.getComponentsStream(realm.getId(), UserStorageProvider.class.getName())
                .collect(Collectors.toList());
        if (components.size() != 1) {
            log.errorf("Expected 1 LDAP Provider, but found: %d providers", components.size());
            throw new HandledException();
        }
        ComponentModel ldapModel = components.get(0);

        // Check that street mapper exists. It's required for now, so that "street" attribute is written to the LDAP
        getMapperModel(realm, ldapModel, "streetMapper");
        ComponentModel groupMapperModel = getMapperModel(realm, ldapModel, "groupsMapper");

        // Create users
        Set<String> createdUserDNs = new HashSet<>();
        BatchTaskRunner.runInBatches(startOffsetUsers, countUsers, batchCount, session.getKeycloakSessionFactory(),
                (KeycloakSession kcSession, int firstIt, int countInIt) -> {

                    LDAPStorageProvider ldapProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, ldapModel);
                    RealmModel appRealm = session.realms().getRealmByName(realmName);

                    for (int i=firstIt ; i<firstIt+countInIt  ; i++) {
                        String username = "user-" + i;
                        String firstName = "John-" + i;
                        String lastName = "Doe-" + i;
                        String email = "user" + i + "@email.cz";
                        LDAPObject createdUser = addLDAPUser(ldapProvider, appRealm, username, firstName, lastName, email, groupsDn, startOffsetGroups, countGroups);
                        createdUserDNs.add(createdUser.getDn().toString());
                    }

                    log.infof("Created LDAP users from: %d to %d", firstIt, firstIt + countInIt -1);

                });


        // Create groups
        BatchTaskRunner.runInBatches(startOffsetGroups, countGroups, batchCount, session.getKeycloakSessionFactory(),
                (KeycloakSession kcSession, int firstIt, int countInIt) -> {

                    LDAPStorageProvider ldapProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, ldapModel);
                    RealmModel appRealm = session.realms().getRealmByName(realmName);
                    GroupLDAPStorageMapper groupMapper = (GroupLDAPStorageMapper) session.getProvider(LDAPStorageMapper.class, groupMapperModel);

                    for (int i=firstIt ; i<firstIt+countInIt  ; i++) {
                        String groupName = "group" + i;

                        Map<String, Set<String>> groupAttrs = new HashMap<>();
                        groupAttrs.put("member", new HashSet<>(createdUserDNs));

                        groupMapper.createLDAPGroup(groupName, groupAttrs);
                    }

                    log.infof("Created LDAP groups from: %d to %d", firstIt, firstIt + countInIt -1);

                });
    }


    private ComponentModel getMapperModel(RealmModel realm, ComponentModel ldapModel, String mapperName) {
        Optional<ComponentModel> first = realm.getComponentsStream(ldapModel.getId(), LDAPStorageMapper.class.getName())
                .filter(component -> Objects.equals(component.getName(), mapperName))
                .findFirst();

        if (first.isPresent()) {
            return first.get();
        } else {
            log.errorf("Not present LDAP mapper called '%s'", mapperName);
            throw new RuntimeException();
        }
    }



    private static LDAPObject addLDAPUser(LDAPStorageProvider ldapProvider, RealmModel realm, final String username,
                                         final String firstName, final String lastName, final String email,
                                         String groupsDN, int startOffsetGroups, int countGroups) {

        UserModel helperUser = new UserModelDelegate(null) {

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getFirstName() {
                return firstName;
            }

            @Override
            public String getLastName() {
                return lastName;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public Stream<String> getAttributeStream(String name) {
                if (UserModel.FIRST_NAME.equals(name)) {
                    return Stream.of(firstName);
                } else if (UserModel.LAST_NAME.equals(name)) {
                    return Stream.of(lastName);
                } else if (UserModel.EMAIL.equals(name)) {
                    return Stream.of(email);
                } else if (UserModel.USERNAME.equals(name)) {
                    return Stream.of(username);
                } else if ("street".equals(name)) {
                    Stream.Builder<String> builder = Stream.builder();
                    for (int i = startOffsetGroups; i < startOffsetGroups + countGroups; i++) {
                        String groupName = "group" + i;
                        LDAPDn groupDn = LDAPDn.fromString(groupsDN);
                        groupDn.addFirst("cn", groupName);
                        builder.add(groupDn.toString());
                    }
                    return builder.build();
                } else {
                    return Stream.empty();
                }
            }
        };
        return LDAPUtils.addUserToLDAP(ldapProvider, realm, helperUser);
    }

}
