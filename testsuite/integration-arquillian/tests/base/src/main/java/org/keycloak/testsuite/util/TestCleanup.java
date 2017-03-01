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

package org.keycloak.testsuite.util;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;

/**
 * Enlist resources to be cleaned after test method
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestCleanup {

    private final Keycloak adminClient;
    private final String realmName;


    private List<String> identityProviderAliases;
    private List<String> userIds;
    private List<String> componentIds;
    private List<String> clientUuids;
    private List<String> roleIds;
    private List<String> groupIds;
    private List<String> authFlowIds;
    private List<String> authConfigIds;

    public TestCleanup(Keycloak adminClient, String realmName) {
        this.adminClient = adminClient;
        this.realmName = realmName;
    }


    public void addUserId(String userId) {
        if (userIds == null) {
            userIds = new LinkedList<>();
        }
        userIds.add(userId);
    }


    public void addIdentityProviderAlias(String identityProviderAlias) {
        if (identityProviderAliases == null) {
            identityProviderAliases = new LinkedList<>();
        }
        identityProviderAliases.add(identityProviderAlias);
    }


    public void addComponentId(String componentId) {
        if (componentIds == null) {
            componentIds = new LinkedList<>();
        }
        componentIds.add(componentId);
    }


    public void addClientUuid(String clientUuid) {
        if (clientUuids == null) {
            clientUuids = new LinkedList<>();
        }
        clientUuids.add(clientUuid);
    }


    public void addRoleId(String roleId) {
        if (roleIds == null) {
            roleIds = new LinkedList<>();
        }
        roleIds.add(roleId);
    }


    public void addGroupId(String groupId) {
        if (groupIds == null) {
            groupIds = new LinkedList<>();
        }
        groupIds.add(groupId);
    }


    public void addAuthenticationFlowId(String flowId) {
        if (authFlowIds == null) {
            authFlowIds = new LinkedList<>();
        }
        authFlowIds.add(flowId);
    }


    public void addAuthenticationConfigId(String executionConfigId) {
        if (authConfigIds == null) {
            authConfigIds = new LinkedList<>();
        }
        authConfigIds.add(executionConfigId);
    }


    public void executeCleanup() {
        RealmResource realm = adminClient.realm(realmName);

        if (userIds != null) {
            for (String userId : userIds) {
                try {
                    realm.users().get(userId).remove();
                } catch (NotFoundException nfe) {
                    // User might be already deleted in the test
                }
            }
        }

        if (identityProviderAliases != null) {
            for (String idpAlias : identityProviderAliases) {
                try {
                    realm.identityProviders().get(idpAlias).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (componentIds != null) {
            for (String componentId : componentIds) {
                try {
                    realm.components().component(componentId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (clientUuids != null) {
            for (String clientUuId : clientUuids) {
                try {
                    realm.clients().get(clientUuId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (roleIds != null) {
            for (String roleId : roleIds) {
                try {
                    realm.rolesById().deleteRole(roleId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (groupIds != null) {
            for (String groupId : groupIds) {
                try {
                    realm.groups().group(groupId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (authFlowIds != null) {
            for (String flowId : authFlowIds) {
                try {
                    realm.flows().deleteFlow(flowId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        if (authConfigIds != null) {
            for (String configId : authConfigIds) {
                try {
                    realm.flows().removeAuthenticatorConfig(configId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }
    }

}
