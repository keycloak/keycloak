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

package org.keycloak.storage.ldap.mappers.freeipa;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;

/**
 * @author <a href="mailto:azamat.hackimov@gmail.com">Azamat H. Hackimov</a>
 */
public class FreeIPAUserAccountControlStorageMapperFactory extends AbstractLDAPStorageMapperFactory {

	public static final String PROVIDER_ID = LDAPConstants.FREEIPA_USER_ACCOUNT_CONTROL_MAPPER;
	protected static final List<ProviderConfigProperty> configProperties;

	static {
		configProperties = getConfigProps(null);
	}

	private static List<ProviderConfigProperty> getConfigProps(ComponentModel parentModel) {
		UserStorageProviderModel parent = parentModel != null ? new UserStorageProviderModel(parentModel) : new UserStorageProviderModel();
		if (parent.isImportEnabled()) {
			ProviderConfigurationBuilder config = ProviderConfigurationBuilder.create()
					.property().name(FreeIPAUserAccountControlStorageMapper.ALWAYS_READ_ENABLED_VALUE_FROM_LDAP).label("Always Read Enabled Value From LDAP")
					.helpText("If on, the user enabled/disabled state will always be read from FreeIPA by checking the proper nsAccountLock")
					.type(ProviderConfigProperty.BOOLEAN_TYPE).defaultValue("false").add();

			return config.build();
		}
		return new ArrayList<>();
	}

	@Override
	public String getHelpText() {
		return "Mapper specific to FreeIPA. It's able to integrate the FreeIPA user account state into Keycloak account state (account enabled). It's using nsAccountLock FreeIPA attribute for that. " +
				"For example if krbPasswordExpiration has passed, the Keycloak user is required to update password, if nsAccountLock is TRUE (disabled account) the Keycloak user is disabled as well etc. Mapper is also able to handle exception code from LDAP user authentication.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties(RealmModel realm, ComponentModel parent) {
		return getConfigProps(parent);
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
		return new FreeIPAUserAccountControlStorageMapper(mapperModel, federationProvider);
	}
}
