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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.ConcurrentMultivaluedHashMap;
import org.keycloak.testsuite.arquillian.TestContext;

import com.google.common.collect.Streams;

/**
 * Enlist resources to be cleaned after test method
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TestCleanup {

    private static final String IDENTITY_PROVIDER_ALIASES = "IDENTITY_PROVIDER_ALIASES";
    private static final String USER_IDS = "USER_IDS";
    private static final String COMPONENT_IDS = "COMPONENT_IDS";
    private static final String CLIENT_UUIDS = "CLIENT_UUIDS";
    private static final String CLIENT_SCOPE_IDS = "CLIENT_SCOPE_IDS";
    private static final String ROLE_IDS = "ROLE_IDS";
    private static final String GROUP_IDS = "GROUP_IDS";
    private static final String AUTH_FLOW_IDS = "AUTH_FLOW_IDS";
    private static final String AUTH_CONFIG_IDS = "AUTH_CONFIG_IDS";
    private static final String REQUIRED_ACTION_ALIASES = "REQUIRED_ACTION_PROVIDERS";
    private static final String LOCALIZATION_LANGUAGES = "LOCALIZATION_LANGUAGES";

    private final TestContext testContext;
    private final String realmName;
    private final ConcurrentLinkedDeque<Runnable> genericCleanups = new ConcurrentLinkedDeque<>();

    // Key is kind of entity (eg. "client", "role", "user" etc), Values are all IDs of entities of given type to cleanup
    private final ConcurrentMultivaluedHashMap<String, String> entities = new ConcurrentMultivaluedHashMap<>();


    public TestCleanup(TestContext testContext, String realmName) {
        this.testContext = testContext;
        this.realmName = realmName;
    }


    public TestCleanup addCleanup(AutoCloseable c) {
        genericCleanups.add(() -> {
            try {
                c.close();
            } catch (Exception ex) {
                // ignore
            }
        });
        return this;
    }

    public void addUserId(String userId) {
        entities.add(USER_IDS, userId);
    }


    public void addIdentityProviderAlias(String identityProviderAlias) {
        entities.add(IDENTITY_PROVIDER_ALIASES, identityProviderAlias);
    }


    public void addComponentId(String componentId) {
        entities.add(COMPONENT_IDS, componentId);
    }


    public void addClientUuid(String clientUuid) {
        entities.add(CLIENT_UUIDS, clientUuid);
    }


    public void addClientScopeId(String clientScopeId) {
        entities.add(CLIENT_SCOPE_IDS, clientScopeId);
    }

    public void addRoleId(String roleId) {
        entities.add(ROLE_IDS, roleId);
    }


    public void addGroupId(String groupId) {
        entities.add(GROUP_IDS, groupId);
    }


    public void addAuthenticationFlowId(String flowId) {
        entities.add(AUTH_FLOW_IDS, flowId);
    }

    public void addLocalization(String language) {
        entities.add(LOCALIZATION_LANGUAGES, language);
    }

    public void addAuthenticationConfigId(String executionConfigId) {
        entities.add(AUTH_CONFIG_IDS, executionConfigId);
    }

    public void addRequiredAction(String alias) {
        entities.add(REQUIRED_ACTION_ALIASES, alias);
    }

    public void executeCleanup() {
        RealmResource realm = getAdminClient().realm(realmName);

        Streams.stream(this.genericCleanups.descendingIterator()).forEach(Runnable::run);

        List<String> userIds = entities.get(USER_IDS);
        if (userIds != null) {
            for (String userId : userIds) {
                try {
                    realm.users().get(userId).remove();
                } catch (NotFoundException nfe) {
                    // User might be already deleted in the test
                }
            }
        }

        List<String> identityProviderAliases = entities.get(IDENTITY_PROVIDER_ALIASES);
        if (identityProviderAliases != null) {
            for (String idpAlias : identityProviderAliases) {
                try {
                    realm.identityProviders().get(idpAlias).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> componentIds = entities.get(COMPONENT_IDS);
        if (componentIds != null) {
            for (String componentId : componentIds) {
                try {
                    realm.components().component(componentId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> clientUuids = entities.get(CLIENT_UUIDS);
        if (clientUuids != null) {
            for (String clientUuId : clientUuids) {
                try {
                    realm.clients().get(clientUuId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        // Client scopes should be after clients
        List<String> clientScopeIds = entities.get(CLIENT_SCOPE_IDS);
        if (clientScopeIds != null) {
            for (String clientScopeId : clientScopeIds) {
                try {
                    realm.clientScopes().get(clientScopeId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> roleIds = entities.get(ROLE_IDS);
        if (roleIds != null) {
            for (String roleId : roleIds) {
                try {
                    realm.rolesById().deleteRole(roleId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> groupIds = entities.get(GROUP_IDS);
        if (groupIds != null) {
            for (String groupId : groupIds) {
                try {
                    realm.groups().group(groupId).remove();
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> authFlowIds = entities.get(AUTH_FLOW_IDS);
        if (authFlowIds != null) {
            for (String flowId : authFlowIds) {
                try {
                    realm.flows().deleteFlow(flowId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> authConfigIds = entities.get(AUTH_CONFIG_IDS);
        if (authConfigIds != null) {
            for (String configId : authConfigIds) {
                try {
                    realm.flows().removeAuthenticatorConfig(configId);
                } catch (NotFoundException nfe) {
                    // Idp might be already deleted in the test
                }
            }
        }

        List<String> localizationLanguages = entities.get(LOCALIZATION_LANGUAGES);
        if (localizationLanguages != null) {
            for (String localizationLanguage : localizationLanguages) {
                try {
                    realm.localization().deleteRealmLocalizationTexts(localizationLanguage);
                } catch (NotFoundException nfe) {
                    // Localization texts might be already deleted in the test
                }
            }
        }

        List<String> requiredActionAliases = entities.get(REQUIRED_ACTION_ALIASES);
        if (requiredActionAliases != null) {
            for (String alias : requiredActionAliases) {
                try {
                    realm.flows().removeRequiredAction(alias);
                } catch (NotFoundException nfe) {
                    // required action might be already deleted in the test
                }
            }
        }
    }

    private Keycloak getAdminClient() {
        return testContext.getAdminClient();
    }

}
