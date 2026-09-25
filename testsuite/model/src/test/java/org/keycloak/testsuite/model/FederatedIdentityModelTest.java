/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.testsuite.broker.oidc.TestKeycloakOidcIdentityProviderFactory;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThrows;

/**
 * @author Réda Housni Alaoui
 */
@RequireProvider(value = IdentityProvider.class, only = TestKeycloakOidcIdentityProviderFactory.ID)
public class FederatedIdentityModelTest extends KeycloakModelTest {

	private static final String IDENTITY_PROVIDER_ALIAS = "idp-test";
	private static final String USERNAME = "jdoe";
	private static final String STORAGE_USER_ID = "f:test-storage:user-1";
	private static final String BROKER_USER_ID = "broker-user-1";
	private static final String BROKER_USERNAME = "jdoe-broker";
	private String realmId;
	private String userId;

	@Override
	public void createEnvironment(KeycloakSession s) {
		RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
		realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));

		this.realmId = realm.getId();

		IdentityProviderFactory identityProviderFactory = (IdentityProviderFactory) s.getKeycloakSessionFactory()
				.getProviderFactory(IdentityProvider.class, TestKeycloakOidcIdentityProviderFactory.ID);

		IdentityProviderModel identityProviderModel = identityProviderFactory.createConfig();
		identityProviderModel.setAlias(IDENTITY_PROVIDER_ALIAS);
        s.identityProviders().create(identityProviderModel);

		userId = s.users().addUser(realm, USERNAME).getId();
	}

	@Override
	public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
		s.realms().removeRealm(realmId);
	}

	@Test
	public void addFederatedIdentity() {

		List<FederatedIdentityModel.FederatedIdentityCreatedEvent> recordedEvents = new ArrayList<>();
		ProviderEventListener providerEventListener = event -> {
			if (event instanceof FederatedIdentityModel.FederatedIdentityCreatedEvent) {
				recordedEvents.add((FederatedIdentityModel.FederatedIdentityCreatedEvent) event);
			}
		};
		getFactory().register(providerEventListener);
		try {
			withRealm(realmId, (session, realm) -> {
				FederatedIdentityModel federatedIdentity = new FederatedIdentityModel(IDENTITY_PROVIDER_ALIAS, userId, USERNAME);
				UserModel user = session.users().getUserById(realm, userId);
				session.users().addFederatedIdentity(realm, user, federatedIdentity);

				assertThat(recordedEvents, hasSize(1));
				FederatedIdentityModel.FederatedIdentityCreatedEvent event = recordedEvents.get(0);
				assertThat(event.getKeycloakSession(), equalTo(session));
				assertThat(event.getRealm(), equalTo(realm));
				assertThat(event.getUser(), equalTo(user));
				assertThat(event.getFederatedIdentity(), equalTo(federatedIdentity));

				return null;
			});
		} finally {
			getFactory().unregister(providerEventListener);
		}
	}

	@Test
	public void removeFederatedIdentity() {
		List<FederatedIdentityModel.FederatedIdentityRemovedEvent> recordedEvents = new ArrayList<>();
		ProviderEventListener providerEventListener = event -> {
			if (event instanceof FederatedIdentityModel.FederatedIdentityRemovedEvent) {
				recordedEvents.add((FederatedIdentityModel.FederatedIdentityRemovedEvent) event);
			}
		};
		getFactory().register(providerEventListener);
		try {
			withRealm(realmId, (session, realm) -> {
				FederatedIdentityModel federatedIdentity = new FederatedIdentityModel(IDENTITY_PROVIDER_ALIAS, userId, USERNAME);
				UserModel user = session.users().getUserById(realm, userId);
				session.users().addFederatedIdentity(realm, user, federatedIdentity);

				session.users().removeFederatedIdentity(realm, user, IDENTITY_PROVIDER_ALIAS);

				assertThat(recordedEvents, hasSize(1));
				FederatedIdentityModel.FederatedIdentityRemovedEvent event = recordedEvents.get(0);
				assertThat(event.getKeycloakSession(), equalTo(session));
				assertThat(event.getRealm(), equalTo(realm));
				assertThat(event.getUser(), equalTo(user));
				assertThat(event.getFederatedIdentity(), equalTo(federatedIdentity));

				return null;
			});
		} finally {
			getFactory().unregister(providerEventListener);
		}
	}

	@Test
	@RequireProvider(UserFederatedStorageProvider.class)
	public void addExistingFederatedIdentityThrowsModelDuplicateException() {
		FederatedIdentityModel link = new FederatedIdentityModel(IDENTITY_PROVIDER_ALIAS, BROKER_USER_ID, BROKER_USERNAME);

		withRealm(realmId, (session, realm) -> {
			UserStorageUtil.userFederatedStorage(session).addFederatedIdentity(realm, STORAGE_USER_ID, link);
			return null;
		});

		withRealm(realmId, (session, realm) -> {
			UserFederatedStorageProvider federatedStorage = UserStorageUtil.userFederatedStorage(session);
			assertThrows(ModelDuplicateException.class,
					() -> federatedStorage.addFederatedIdentity(realm, STORAGE_USER_ID, link));
			return null;
		});

		withRealm(realmId, (session, realm) -> {
			assertThat(UserStorageUtil.userFederatedStorage(session).getFederatedIdentitiesStream(STORAGE_USER_ID, realm).count(), equalTo(1L));
			return null;
		});
	}

	@Test
	@RequireProvider(UserFederatedStorageProvider.class)
	public void concurrentAddFederatedIdentityCreatesSingleLink() throws InterruptedException {
		int numberOfThreads = 4;
		AtomicInteger created = new AtomicInteger();
		AtomicInteger duplicates = new AtomicInteger();
		FederatedIdentityModel link = new FederatedIdentityModel(IDENTITY_PROVIDER_ALIAS, BROKER_USER_ID, BROKER_USERNAME);

		inIndependentFactories(numberOfThreads, 60, () -> {
			try {
				inComittedTransaction(session -> {
					RealmModel realm = session.realms().getRealm(realmId);
					session.getContext().setRealm(realm);
					UserStorageUtil.userFederatedStorage(session).addFederatedIdentity(realm, STORAGE_USER_ID, link);
				});
				created.incrementAndGet();
			} catch (ModelDuplicateException e) {
				duplicates.incrementAndGet();
			}
		});

		assertThat(created.get(), equalTo(1));
		assertThat(duplicates.get(), equalTo(numberOfThreads - 1));

		withRealm(realmId, (session, realm) -> {
			assertThat(UserStorageUtil.userFederatedStorage(session).getFederatedIdentitiesStream(STORAGE_USER_ID, realm).count(), equalTo(1L));
			return null;
		});
	}

}
