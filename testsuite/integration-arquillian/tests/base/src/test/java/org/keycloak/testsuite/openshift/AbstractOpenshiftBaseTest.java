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
package org.keycloak.testsuite.openshift;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.protocol.openshift.clientstorage.OpenshiftClientStorageProviderFactory;
import org.keycloak.protocol.openshift.connections.rest.OpenshiftClient;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;

import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractOpenshiftBaseTest extends AbstractTestRealmKeycloakTest {
    /*
    $ oc create sa keycloak
    $ oc adm policy add-cluster-role-to-user system:auth-delegator -z keycloak
    $ oc adm policy add-cluster-role-to-user system:master -z keycloak
    $ oc describe sa keycloak
    # look for token and describe it
    $ oc describe secret xxxxx-xxx
    # copy token to MASTER_TOKEN constant above
     */
    protected String addComponent(ComponentRepresentation component) {
        Response resp = adminClient.realm("test").components().add(component);
        resp.close();
        String id = ApiUtil.getCreatedId(resp);
        getCleanup().addComponentId(id);
        return id;
    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        List<ComponentRepresentation> reps = adminClient.realm("test").components().query(null, ClientStorageProvider.class.getName());
        if (reps.size() > 0) return;
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("openshift oauth client provider");
        provider.setProviderId(OpenshiftClientStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(ClientStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.ACCESS_TOKEN, getMasterToken());
        provider.getConfig().putSingle(OpenshiftClientStorageProviderFactory.OPENSHIFT_URI, getOpenshiftUrl());

        addComponent(provider);
    }

    public static Properties config = new Properties();

    public static final String OPENSHIFT_CONFIG = "openshift.config";

    @BeforeClass
    public static void loadConfig() throws Exception {
        Assume.assumeTrue(System.getProperties().containsKey(OPENSHIFT_CONFIG));
        config.load(new FileInputStream(System.getProperty(OPENSHIFT_CONFIG)));
        //config.put("master_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJvcGVuc2hpZnQtd2ViLWNvbnNvbGUiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlY3JldC5uYW1lIjoia2V5Y2xvYWstdG9rZW4tcHFmcWQiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoia2V5Y2xvYWsiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI4MmU5ZTE0YS0zYWRjLTExZTgtYmZkMy0wMDBjMjk4MmM5YjgiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6b3BlbnNoaWZ0LXdlYi1jb25zb2xlOmtleWNsb2FrIn0.MP84QI0HVg6Xnk1sZhmzcXbDoN0mAdFZm061_tdyjBlqR34oKKHeZ_4q_kja7xZnmg3t0a7ZZ2mW0mafVVXwjiDucdYMkdJ5vEmi291kNz3Qut9hS8dqWBY0tt4hnwzjDr_4O6mfO66CFdjLs_9uJfqXFQpH8xi4RI98fPAqMAiqQ69tt9SVkt7ozTJI-qCSADjFlZeWnWSGP8ruX5lI9URCSlpfSiDdZeXzU6WQUh7pdjbq2mp64azfg8Nv5RfQQ5w9ZFMEhFqoJNelO5UVIN6JdpjrpLbsm5TGDIg68_RhCcdWY_lGDjE-gE2_qxulIfbKHyruKxU5uolgLaCIAw");
        //config.put("master_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJteXByb2plY3QiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlY3JldC5uYW1lIjoia2V5Y2xvYWstdG9rZW4tY2M4cWwiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoia2V5Y2xvYWsiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIzMzg0NTU3OS0zYTg5LTExZTgtOGQ2Mi0wMDBjMjk4MmM5YjgiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6bXlwcm9qZWN0OmtleWNsb2FrIn0.KDsjSh5Mai43Q9oj9Zeokz6AphGHEV9TdhTK9y7qj-F976gUhQw7dmNOAs22m7CPjbgpZHlom6KZD0-yUP4_ZgE6r6kKw_lN_8YvM_4sMG_bZNrl9y5nk5tIrRsIexQ-inXo9AxnlMPTGClZTfePMOmoP6Ubz9OMvxZLL066kKNVRhoE0yE-Nv1o1GHnnYmx9q-LEnL5IxSmI1_fT28bceDgisxhopVAoQoFX7T5TmvBnMoZwJ6ba3ZQm7z0ISIf-ur1W77-NbYLOx9lepiT4si5OSO9qdzfUQjBc6YL8-ueU_QrmrftJYwc9tvvLOo2b0jndd6N1ApNnyx1niSdaA");
        //config.put("openshift_url", "https://192.168.238.132:8443");
    }

    public static String getMasterToken() {
        return config.getProperty("master_token");
    }

    public static String getOpenshiftUrl() {
        return config.getProperty("openshift_url");
    }

    public static OpenshiftClient createOpenshiftClient() {
        return OpenshiftClient.instance(AbstractOpenshiftBaseTest.getOpenshiftUrl(), AbstractOpenshiftBaseTest.getMasterToken());
    }




}
