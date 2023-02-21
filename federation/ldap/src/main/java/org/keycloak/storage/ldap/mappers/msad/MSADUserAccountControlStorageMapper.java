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

package org.keycloak.storage.ldap.mappers.msad;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;
import org.keycloak.storage.ldap.mappers.PasswordUpdateCallback;
import org.keycloak.storage.ldap.mappers.TxAwareLDAPUserModelDelegate;

import javax.naming.AuthenticationException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mapper specific to MSAD. It's able to read the userAccountControl and pwdLastSet attributes and set actions in Keycloak based on that.
 * It's also able to handle exception code from LDAP user authentication (See http://www-01.ibm.com/support/docview.wss?uid=swg21290631 )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MSADUserAccountControlStorageMapper extends AbstractLDAPStorageMapper implements PasswordUpdateCallback {

    public static final String LDAP_PASSWORD_POLICY_HINTS_ENABLED = "ldap.password.policy.hints.enabled";

    private static final Logger logger = Logger.getLogger(MSADUserAccountControlStorageMapper.class);

    private static final Pattern AUTH_EXCEPTION_REGEX = Pattern.compile(".*AcceptSecurityContext error, data ([0-9a-f]*), v.*");
    private static final Pattern AUTH_INVALID_NEW_PASSWORD = Pattern.compile(".*ERROR CODE ([0-9A-F]+) - ([0-9A-F]+): .*WILL_NOT_PERFORM.*");

    public MSADUserAccountControlStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
        ldapProvider.setUpdater(this);
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        query.addReturningLdapAttribute(LDAPConstants.PWD_LAST_SET);
        query.addReturningLdapAttribute(LDAPConstants.USER_ACCOUNT_CONTROL);

        // This needs to be read-only and can be set to writable just on demand
        query.addReturningReadOnlyLdapAttribute(LDAPConstants.PWD_LAST_SET);

        if (ldapProvider.getEditMode() != UserStorageProvider.EditMode.WRITABLE) {
            query.addReturningReadOnlyLdapAttribute(LDAPConstants.USER_ACCOUNT_CONTROL);
        }
    }

    @Override
    public LDAPOperationDecorator beforePasswordUpdate(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
        // Not apply policies if password is reset by admin (not by user themself)
        if (password.isAdminRequest()) {
            return null;
        }

        boolean applyDecorator = mapperModel.get(LDAP_PASSWORD_POLICY_HINTS_ENABLED, false);
        return applyDecorator ? new LDAPServerPolicyHintsDecorator() : null;
    }

    @Override
    public void passwordUpdated(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
        logger.debugf("Going to update userAccountControl for ldap user '%s' after successful password update. Keycloak user '%s' in realm '%s'", ldapUser.getDn().toString(),
                user.getUsername(), getRealmName());

        // Normally it's read-only
        ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

        ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "-1");

        UserAccountControl control = getUserAccountControl(ldapUser);
        control.remove(UserAccountControl.PASSWD_NOTREQD);
        control.remove(UserAccountControl.PASSWORD_EXPIRED);

        if (user.isEnabled()) {
            control.remove(UserAccountControl.ACCOUNTDISABLE);
        }

        updateUserAccountControl(true, ldapUser, control);
    }

    @Override
    public void passwordUpdateFailed(UserModel user, LDAPObject ldapUser, UserCredentialModel password, ModelException exception) {
        throw processFailedPasswordUpdateException(exception);
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return new MSADUserModelDelegate(delegate, ldapUser);
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {

    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {

    }

    @Override
    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        String exceptionMessage = ldapException.getMessage();
        Matcher m = AUTH_EXCEPTION_REGEX.matcher(exceptionMessage);
        if (m.matches()) {
            String errorCode = m.group(1);
            return processAuthErrorCode(errorCode, user);
        } else {
            return false;
        }
    }

    protected boolean processAuthErrorCode(String errorCode, UserModel user) {
        logger.debugf("MSAD Error code is '%s' after failed LDAP login of user '%s'. Realm is '%s'", errorCode, user.getUsername(), getRealmName());

        if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
            if (errorCode.equals("532") || errorCode.equals("773")) {
                // User needs to change his MSAD password. Allow him to login, but add UPDATE_PASSWORD required action to authenticationSession
                if (user.getRequiredActionsStream().noneMatch(action -> Objects.equals(action, UserModel.RequiredAction.UPDATE_PASSWORD.name()))) {
                    // This usually happens when 532 was returned, which means that "pwdLastSet" is set to some positive value, which is older than MSAD password expiration policy.
                    AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
                    if (authSession != null) {
                        if (authSession.getRequiredActions().stream().noneMatch(action -> Objects.equals(action, UserModel.RequiredAction.UPDATE_PASSWORD.name()))) {
                            logger.debugf("Adding requiredAction UPDATE_PASSWORD to the authenticationSession of user %s", user.getUsername());
                            authSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                        }
                    } else {
                        // Just a fallback. It should not happen during normal authentication process
                        logger.debugf("Adding requiredAction UPDATE_PASSWORD to the user %s", user.getUsername());
                        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                    }
                } else {
                    // This usually happens when "773" error code is returned by MSAD. This typically happens when "pwdLastSet" is set to 0 and password was manually set
                    // by administrator (or user) to expire
                    logger.tracef("Skip adding required action UPDATE_PASSWORD. It was already set on user '%s' in realm '%s'", user.getUsername(), getRealmName());
                }
                return true;
            } else if (errorCode.equals("533")) {
                // User is disabled in MSAD. Set him to disabled in KC as well
                if (user.isEnabled()) {
                    user.setEnabled(false);
                }
                return true;
            } else if (errorCode.equals("775")) {
                logger.warnf("Locked user '%s' attempt to login. Realm is '%s'", user.getUsername(), getRealmName());
            }
        }

        return false;
    }


    protected ModelException processFailedPasswordUpdateException(ModelException e) {
        if (e.getCause() == null || e.getCause().getMessage() == null) {
            return e;
        }

        String exceptionMessage = e.getCause().getMessage().replace('\n', ' ');
        logger.debugf("Failed to update password in Active Directory. Exception message: %s", exceptionMessage);
        exceptionMessage = exceptionMessage.toUpperCase();

        Matcher m = AUTH_INVALID_NEW_PASSWORD.matcher(exceptionMessage);
        if (m.matches()) {
            String errorCode = m.group(1);
            String errorCode2 = m.group(2);

            // 52D corresponds to ERROR_PASSWORD_RESTRICTION. See https://msdn.microsoft.com/en-us/library/windows/desktop/ms681385(v=vs.85).aspx
            if ((errorCode.equals("53")) && errorCode2.endsWith("52D")) {
                ModelException me = new ModelException("invalidPasswordGenericMessage", e);
                return me;
            }
        }

        return e;
    }

    protected UserAccountControl getUserAccountControl(LDAPObject ldapUser) {
        String userAccountControl = ldapUser.getAttributeAsString(LDAPConstants.USER_ACCOUNT_CONTROL);
        long longValue = userAccountControl == null ? 0 : Long.parseLong(userAccountControl);
        return new UserAccountControl(longValue);
    }

    // Update user in LDAP if "updateInLDAP" is true. Otherwise it is assumed that LDAP update will be called at the end of transaction
    protected void updateUserAccountControl(boolean updateInLDAP, LDAPObject ldapUser, UserAccountControl accountControl) {
        String userAccountControlValue = String.valueOf(accountControl.getValue());
        logger.debugf("Updating userAccountControl of user '%s' to value '%s'. Realm is '%s'", ldapUser.getDn().toString(), userAccountControlValue, getRealmName());

        ldapUser.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, userAccountControlValue);

        if (updateInLDAP) {
            ldapProvider.getLdapIdentityStore().update(ldapUser);
        }
    }

    private String getRealmName() {
        RealmModel realm = session.getContext().getRealm();
        return (realm != null) ? realm.getName() : "null";
    }


    public class MSADUserModelDelegate extends TxAwareLDAPUserModelDelegate {

        private final LDAPObject ldapUser;

        public MSADUserModelDelegate(UserModel delegate, LDAPObject ldapUser) {
            super(delegate, ldapProvider, ldapUser);
            this.ldapUser = ldapUser;
        }

        @Override
        public boolean isEnabled() {
            boolean kcEnabled = super.isEnabled();

            if (getPwdLastSet() > 0) {
                // Merge KC and MSAD
                return kcEnabled && !getUserAccountControl(ldapUser).has(UserAccountControl.ACCOUNTDISABLE);
            } else {
                // If new MSAD user is created and pwdLastSet is still 0, MSAD account is in disabled state. So read just from Keycloak DB. User is not able to login via MSAD anyway
                return kcEnabled;
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            // Always update DB
            super.setEnabled(enabled);

            if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && getPwdLastSet() > 0) {
                MSADUserAccountControlStorageMapper.logger.debugf("Going to propagate enabled=%s for ldapUser '%s' to MSAD", enabled, ldapUser.getDn().toString());

                UserAccountControl control = getUserAccountControl(ldapUser);
                if (enabled) {
                    control.remove(UserAccountControl.ACCOUNTDISABLE);
                } else {
                    control.add(UserAccountControl.ACCOUNTDISABLE);
                }

                markUpdatedAttributeInTransaction(LDAPConstants.ENABLED);

                updateUserAccountControl(false, ldapUser, control);
            }
        }

        @Override
        public void addRequiredAction(RequiredAction action) {
            String actionName = action.name();
            addRequiredAction(actionName);
        }

        @Override
        public void addRequiredAction(String action) {
            if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && RequiredAction.UPDATE_PASSWORD.toString().equals(action)) {
                MSADUserAccountControlStorageMapper.logger.debugf("Going to propagate required action UPDATE_PASSWORD to MSAD for ldap user '%s'. Keycloak user '%s' in realm '%s'",
                        ldapUser.getDn().toString(), getUsername(), getRealmName());

                // Normally it's read-only
                ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

                ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "0");

                markUpdatedRequiredActionInTransaction(action);
            } else {
                // Update DB
                MSADUserAccountControlStorageMapper.logger.debugf("Going to add required action '%s' of user '%s' in realm '%s' to the DB", action, getUsername(), getRealmName());
                super.addRequiredAction(action);
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

                // Don't set pwdLastSet in MSAD when it is new user
                UserAccountControl accountControl = getUserAccountControl(ldapUser);
                if (accountControl.getValue() != 0 && !accountControl.has(UserAccountControl.PASSWD_NOTREQD)) {
                    MSADUserAccountControlStorageMapper.logger.debugf("Going to remove required action UPDATE_PASSWORD from MSAD for ldap user '%s'. Account control: %s, Keycloak user '%s' in realm '%s'",
                            ldapUser.getDn().toString(), accountControl.getValue(), getUsername(), getRealmName());

                    // Normally it's read-only
                    ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

                    ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "-1");

                    markUpdatedRequiredActionInTransaction(action);
                } else {
                    MSADUserAccountControlStorageMapper.logger.tracef("It was not required action to remove UPDATE_PASSWORD from MSAD for ldap user '%s' as it was not set on the user. Account control: %s, Keycloak user '%s' in realm '%s'",
                            ldapUser.getDn().toString(), accountControl.getValue(), getUsername(), getRealmName());
                }
            }
        }

        @Override
        public Stream<String> getRequiredActionsStream() {
            if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
                if (getPwdLastSet() == 0 || getUserAccountControl(ldapUser).has(UserAccountControl.PASSWORD_EXPIRED)) {
                    MSADUserAccountControlStorageMapper.logger.tracef("Required action UPDATE_PASSWORD is set in LDAP for user '%s' in realm '%s'", getUsername(), getRealmName());
                    return Stream.concat(super.getRequiredActionsStream(), Stream.of(RequiredAction.UPDATE_PASSWORD.toString()))
                            .distinct();
                }
            }
            return super.getRequiredActionsStream();
        }

        protected long getPwdLastSet() {
            String pwdLastSet = ldapUser.getAttributeAsString(LDAPConstants.PWD_LAST_SET);
            return pwdLastSet == null ? 0 : Long.parseLong(pwdLastSet);
        }


    }

}
