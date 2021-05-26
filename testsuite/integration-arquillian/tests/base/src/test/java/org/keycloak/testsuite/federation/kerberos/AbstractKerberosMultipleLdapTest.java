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

package org.keycloak.testsuite.federation.kerberos;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * Contains just helper methods. No test methods.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public abstract class AbstractKerberosMultipleLdapTest extends AbstractKerberosTest {


    @Override
    protected ComponentRepresentation getUserStorageConfiguration() {
        return null;
    }


    protected abstract List<KerberosRule> getUserStoragesConfig();

    protected void changeKerberosExpectedRealm(String userStorageName, String expectedRealm) {
        List<ComponentRepresentation> userStorages = testRealmResource().components().query(testRealmResource().toRepresentation().getId(), UserStorageProvider.class.getCanonicalName());
        ComponentRepresentation userStorage = null;
        for (ComponentRepresentation cr: userStorages) {
            if (userStorageName.equals(cr.getName())) {
                userStorage = cr;
                break;
            }
        }
        if (StringUtils.isEmpty(expectedRealm)) {
            userStorage.getConfig().remove(KerberosConstants.KERBEROS_REALM);
        } else {
            userStorage.getConfig().putSingle(KerberosConstants.KERBEROS_REALM, expectedRealm);
        }
        testRealmResource().components().component(userStorage.getId()).update(userStorage);
    }

    protected ComponentRepresentation getUserStorageConfiguration(KerberosRule kRule) {
        Map<String,String> kerberosConfig = kRule.getConfig();
        MultivaluedHashMap<String, String> config = toComponentConfig(kerberosConfig);

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName(config.getFirst(LDAPTestConfiguration.NAME));
        model.setPriority(Integer.parseInt(config.getFirst(LDAPTestConfiguration.PRIORITY)));
        model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        model.setConfig(config);

        ComponentRepresentation rep = ModelToRepresentation.toRepresentationWithoutConfig(model);
        return rep;
    }

    @Before
    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        for (KerberosRule kRule: getUserStoragesConfig()) {
            ComponentRepresentation rep = getUserStorageConfiguration(kRule);
            Response resp = testRealmResource().components().add(rep);
            getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
            resp.close();
        }
    }
}
