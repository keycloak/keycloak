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

package org.keycloak.testsuite.model;


import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.infinispan.ClientAdapter;
import org.keycloak.models.cache.infinispan.RealmAdapter;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class CacheTest extends AbstractTestRealmKeycloakTest {

	private ClientModel testApp = null;
	private int grantedRolesCount=0;
	private RealmModel realm = null;
	private UserModel user = null;
	
	 @Override
	    public void configureTestRealm(RealmRepresentation testRealm) {
	    }

	 @Test
	    public void testStaleCache() throws Exception {
		 testingClient.server().run(session -> {
		 	String appId = null;
	        {
	            // load up cache

	            RealmModel realm = session.realms().getRealmByName("test");
	            assertTrue(realm instanceof RealmAdapter);
	            ClientModel testApp = realm.getClientByClientId("test-app");
	            assertTrue(testApp instanceof ClientAdapter);
	            assertNotNull(testApp);
	            appId = testApp.getId();
	            assertTrue(testApp.isEnabled());
	     
	        
	       
	            // update realm, then get an AppModel and change it.  The AppModel would not be a cache adapter

	            realm = session.realms().getRealmsStream().filter(r -> {
					assertTrue(r instanceof RealmAdapter);
					if ("test".equals(r.getName()))
						return true;
					return false;
				}).findFirst().orElse(null);

	            assertNotNull(realm);

	            realm.setAccessCodeLifespanLogin(200);
	            testApp = realm.getClientByClientId("test-app");

	            assertNotNull(testApp);
	            testApp.setEnabled(false);
	        
	        
	        // make sure that app cache was flushed and enabled changed
	        
	       
	            realm = session.realms().getRealmByName("test");
	            Assert.assertEquals(200, realm.getAccessCodeLifespanLogin());
	            testApp = session.clients().getClientById(realm, appId);
	            Assert.assertFalse(testApp.isEnabled());
	        
	        }
		 });
		 }
    @Test
    public void testAddUserNotAddedToCache() {

    	testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            UserModel user = session.users().addUser(realm, "testAddUserNotAddedToCache");
            user.setFirstName("firstName");
            user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
    	
            UserSessionModel userSession = session.sessions().createUserSession(UUID.randomUUID().toString(), realm, user, "testAddUserNotAddedToCache",
					"127.0.0.1", "auth", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            user = userSession.getUser();

            user.setLastName("lastName");

            assertNotNull(user.getLastName());
       });
  
    }

    // KEYCLOAK-1842
    @Test
    public void testRoleMappingsInvalidatedWhenClientRemoved() {
      	testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            
            UserModel user = session.users().addUser(realm, "joel");
            ClientModel client = realm.addClient("foo");
            RoleModel fooRole = client.addRole("foo-role");
            user.grantRole(fooRole);
       });

        testingClient.server().run(session -> {  
        	RealmModel realm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername(realm, "joel");
            long grantedRolesCount = user.getRoleMappingsStream().count();

            ClientModel client = realm.getClientByClientId("foo");
            realm.removeClient(client.getId());

            realm = session.realms().getRealmByName("test");
            user = session.users().getUserByUsername(realm, "joel");
        
            Set<RoleModel> roles = user.getRoleMappingsStream().collect(Collectors.toSet());
            for (RoleModel role : roles) {
                Assert.assertNotNull(role.getContainer());
            }
        
            Assert.assertEquals(roles.size(), grantedRolesCount - 1);
        });

    }

}
