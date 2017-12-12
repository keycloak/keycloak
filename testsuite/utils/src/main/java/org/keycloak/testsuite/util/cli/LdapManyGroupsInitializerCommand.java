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
import java.util.Optional;
import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapper;

/**
 * The command requires that:
 * - Realm has 1 LDAP storage provider defined
 * - The LDAP provider has group-mapper named "groupsMapper", with:
 * -- "LDAP Groups DN" pointing to same DN, like this command <groups-dn> .
 * -- It's supposed to PreserveGroupsInheritance on
 *
 * It will create top-groups-count "root" groups and "subgroups-in-every-top-group" groups in every child.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LdapManyGroupsInitializerCommand extends AbstractCommand  {

    @Override
    public String getName() {
        return "createLdapGroups";
    }

    @Override
    public String printUsage() {
        return super.printUsage() + " <realm-name> <groups-dn> <start-offset-top-groups> <top-groups-count> <subgroups-in-every-top-group>.\nSee javadoc of class LdapManyGroupsInitializerCommand for additional details.";
    }

    @Override
    protected void doRunCommand(KeycloakSession session) {
        String realmName = getArg(0);
        String groupsDn = getArg(1);
        int startOffsetTopGroups = getIntArg(2);
        int topGroupsCount = getIntArg(3);
        int subgroupsInEveryGroup = getIntArg(4);

        RealmModel realm = session.realms().getRealmByName(realmName);
        List<ComponentModel> components = realm.getComponents(realm.getId(), UserStorageProvider.class.getName());
        if (components.size() != 1) {
            log.errorf("Expected 1 LDAP Provider, but found: %d providers", components.size());
            throw new HandledException();
        }
        ComponentModel ldapModel = components.get(0);

        // Check that street mapper exists. It's required for now, so that "street" attribute is written to the LDAP
        ComponentModel groupMapperModel = getMapperModel(realm, ldapModel, "groupsMapper");


        // Create groups
        for (int i=startOffsetTopGroups ; i<startOffsetTopGroups+topGroupsCount ; i++) {
            final int iFinal = i;
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {

                LDAPStorageProvider ldapProvider = (LDAPStorageProvider)session.getProvider(UserStorageProvider.class, ldapModel);
                RealmModel appRealm = session.realms().getRealmByName(realmName);
                GroupLDAPStorageMapper groupMapper = (GroupLDAPStorageMapper) session.getProvider(LDAPStorageMapper.class, groupMapperModel);

                Set<String> childGroupDns = new HashSet<>();

                for (int j=0 ; j<subgroupsInEveryGroup ; j++) {
                    String groupName = "group-" + iFinal + "-" + j;
                    LDAPObject createdGroup = groupMapper.createLDAPGroup(groupName, new HashMap<>());
                    childGroupDns.add(createdGroup.getDn().toString());
                }

                String topGroupName = "group-" + iFinal;

                Map<String, Set<String>> groupAttrs = new HashMap<>();
                groupAttrs.put("member", new HashSet<>(childGroupDns));

                groupMapper.createLDAPGroup(topGroupName, groupAttrs);

            });
        }
    }


    private ComponentModel getMapperModel(RealmModel realm, ComponentModel ldapModel, String mapperName) {
        List<ComponentModel> ldapMappers = realm.getComponents(ldapModel.getId(), LDAPStorageMapper.class.getName());
        Optional<ComponentModel> optional = ldapMappers.stream().filter((ComponentModel mapper) -> {
            return mapper.getName().equals(mapperName);
        }).findFirst();

        if (!optional.isPresent()) {
            log.errorf("Not present LDAP mapper called '%s'", mapperName);
            throw new HandledException();
        }

        return optional.get();
    }
}
