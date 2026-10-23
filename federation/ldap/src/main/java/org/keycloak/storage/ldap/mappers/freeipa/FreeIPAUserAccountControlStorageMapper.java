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

import org.jboss.logging.Logger;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;
import org.keycloak.storage.ldap.mappers.PasswordUpdateCallback;
import org.keycloak.storage.ldap.mappers.TxAwareLDAPUserModelDelegate;

import javax.naming.NamingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mapper specific to FreeIPA. It's able to read the nsAccountLock and krbPasswordExpiration attributes and
 * set actions in Keycloak based on that.
 * It's also able to handle exception code from LDAP user authentication.
 *
 * @author <a href="mailto:azamat.hackimov@gmail.com">Azamat H. Hackimov</a>
 */
public class FreeIPAUserAccountControlStorageMapper extends AbstractLDAPStorageMapper implements PasswordUpdateCallback {
	public static final String ALWAYS_READ_ENABLED_VALUE_FROM_LDAP = "always.read.enabled.value.from.ldap";

	private static final Logger logger = Logger.getLogger(FreeIPAUserAccountControlStorageMapper.class);

	private static final Pattern AUTH_EXCEPTION_REGEX = Pattern.compile(".*LDAP: error code ([0-9]*) - (.*).");
	private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmss'Z'";

	public FreeIPAUserAccountControlStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
		super(mapperModel, ldapProvider);
		ldapProvider.setUpdater(this);
	}

	@Override
	public void beforeLDAPQuery(LDAPQuery query) {
		query.addReturningLdapAttribute(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK);
		query.addReturningLdapAttribute(LDAPConstants.FREEIPA_KRB_PASSWORD_EXPIRATION);

		query.addReturningReadOnlyLdapAttribute(LDAPConstants.FREEIPA_KRB_PASSWORD_EXPIRATION);

		if (ldapProvider.getEditMode() != UserStorageProvider.EditMode.WRITABLE) {
			query.addReturningReadOnlyLdapAttribute(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK);
		}
	}

	@Override
	public LDAPOperationDecorator beforePasswordUpdate(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
		return null; // Not supported for now.
	}

	@Override
	public void passwordUpdated(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
		// Add 30 seconds ahead of current time to avoid race between UPDATE_PASSWORD and FreeIPA password policies
		String newExpire = LocalDateTime.now().plusSeconds(30).format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
		ldapUser.setSingleAttribute(LDAPConstants.FREEIPA_KRB_PASSWORD_EXPIRATION, newExpire);

		ldapProvider.getLdapIdentityStore().update(ldapUser);
	}

	@Override
	public void passwordUpdateFailed(UserModel user, LDAPObject ldapUser, UserCredentialModel password, ModelException exception) {
		throw processFailedPasswordUpdateException(exception);
	}

	@Override
	public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
		return new FreeIPAUserAccountControlStorageMapper.FreeIPAUserModelDelegate(delegate, ldapUser, parseBooleanParameter(mapperModel, ALWAYS_READ_ENABLED_VALUE_FROM_LDAP));
	}

	@Override
	public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {

	}

	@Override
	public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
		// Check if user is enabled in FreeIPA or not.
		user.setEnabled(!Boolean.parseBoolean(ldapUser.getAttributeAsString(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK)));
	}

	@Override
	public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, NamingException ldapException, RealmModel realm) {
		return processAuthErrorCode(ldapException.getMessage(), user);
	}

	protected boolean processAuthErrorCode(String exceptionMessage, UserModel user) {
		Matcher m = AUTH_EXCEPTION_REGEX.matcher(exceptionMessage);
		if (m.matches()) {
			String errorCode = m.group(1);
			String errorMessage = m.group(2);

			logger.debugf("FreeIPA Error message is '%s' after failed LDAP login of user '%s'. Realm is '%s'", errorCode, user.getUsername(), getRealmName());

			if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
				if (errorCode.equals("53") && errorMessage.equals("Account inactivated. Contact system administrator.")) {
					logger.warnf("Locked user '%s' attempt to login. Realm is '%s'", user.getUsername(), getRealmName());
					// User is disabled in FreeIPA. Set him to disabled in KC as well
					if (user.isEnabled()) {
						user.setEnabled(false);
					}
					return true;
				}
			}
		}

		return false;
	}

	protected ModelException processFailedPasswordUpdateException(ModelException e) {
		return e;
	}

	private String getRealmName() {
		RealmModel realm = getSession().getContext().getRealm();
		return (realm != null) ? realm.getName() : "null";
	}

	public class FreeIPAUserModelDelegate extends TxAwareLDAPUserModelDelegate {

		private final LDAPObject ldapUser;
		private final boolean isAlwaysReadEnabledFromLdap;

		public FreeIPAUserModelDelegate(UserModel delegate, LDAPObject ldapUser, boolean isAlwaysReadEnabledFromLdap) {
			super(delegate, ldapProvider, ldapUser);
			this.ldapUser = ldapUser;
			this.isAlwaysReadEnabledFromLdap = isAlwaysReadEnabledFromLdap;
		}

		@Override
		public boolean isEnabled() {
			if (isAlwaysReadEnabledFromLdap) {
				return !Boolean.parseBoolean(ldapUser.getAttributeAsString(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK));
			}
			return super.isEnabled();
		}

		@Override
		public void setEnabled(boolean enabled) {
			if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
				if (enabled) {
					logger.debugf("Removing nsAccountLock of user '%s'", ldapUser.getDn().toString());
					// TODO: Use removeAttribute once available
					ldapUser.setSingleAttribute(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK, "FALSE");
				} else {
					logger.debugf("Setting nsAccountLock of user '%s' to value 'TRUE'", ldapUser.getDn().toString());
					ldapUser.setSingleAttribute(LDAPConstants.FREEIPA_NS_ACCOUNT_LOCK, "TRUE");
				}
				ldapProvider.getLdapIdentityStore().update(ldapUser);
			}
			// Always update DB
			super.setEnabled(enabled);
		}

		@Override
		public void addRequiredAction(RequiredAction action) {
			String actionName = action.name();
			addRequiredAction(actionName);
		}

		@Override
		public void addRequiredAction(String action) {
			// Always update DB
			super.addRequiredAction(action);

			if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && RequiredAction.UPDATE_PASSWORD.toString().equals(action)) {
				logger.debugf("Going to propagate required action UPDATE_PASSWORD to FreeIPA for ldap user '%s' ", ldapUser.getDn().toString());
				ldapProvider.getLdapIdentityStore().update(ldapUser);
			}
		}

		@Override
		public void removeRequiredAction(RequiredAction action) {
			String actionName = action.name();
			removeRequiredAction(actionName);
		}

		@Override
		public void removeRequiredAction(String action) {
			// Always update DB
			super.removeRequiredAction(action);

			if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && RequiredAction.UPDATE_PASSWORD.toString().equals(action)) {
				logger.debugf("Going to remove required action UPDATE_PASSWORD from FreeIPA for ldap user '%s' ", ldapUser.getDn().toString());

				ldapProvider.getLdapIdentityStore().update(ldapUser);
			}
		}

		@Override
		public Stream<String> getRequiredActionsStream() {
			if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
				if (isPasswordExpired()) {
					FreeIPAUserAccountControlStorageMapper.logger.tracef("Required action UPDATE_PASSWORD is set in LDAP for user '%s' in realm '%s'",
							getUsername(), getRealmName());
					return Stream.concat(super.getRequiredActionsStream(), Stream.of(RequiredAction.UPDATE_PASSWORD.toString()))
							.distinct();
				}
			}
			return super.getRequiredActionsStream();
		}

		protected boolean isPasswordExpired() {
			String krbPasswordExpiration = ldapUser.getAttributeAsString(LDAPConstants.FREEIPA_KRB_PASSWORD_EXPIRATION);
			FreeIPAUserAccountControlStorageMapper.logger.tracef("Get krbPasswordExpiration for LDAP user '%s' in realm '%s': '%s'",
					getUsername(), getRealmName(), krbPasswordExpiration);
			if (krbPasswordExpiration != null) {
				try {
					LocalDateTime current = LocalDateTime.now();
					LocalDateTime expire = LocalDateTime.parse(krbPasswordExpiration, DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));

					return current.isAfter(expire);
				} catch (DateTimeParseException e) {
					FreeIPAUserAccountControlStorageMapper.logger.warnf("Malformed format of %s for LDAP user '%s' in realm '%s': '%s', expected format '%s'",
							LDAPConstants.FREEIPA_KRB_PASSWORD_EXPIRATION, getUsername(), getRealmName(), TIMESTAMP_FORMAT);
					return false;
				}
			}

			return false;
		}

	}

}
