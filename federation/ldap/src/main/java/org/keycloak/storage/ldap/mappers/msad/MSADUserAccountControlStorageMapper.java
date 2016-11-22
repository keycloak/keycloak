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
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.PasswordUpdated;

import javax.naming.AuthenticationException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapper specific to MSAD. It's able to read the userAccountControl and pwdLastSet attributes and set actions in Keycloak based on that.
 * It's also able to handle exception code from LDAP user authentication (See http://www-01.ibm.com/support/docview.wss?uid=swg21290631 )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MSADUserAccountControlStorageMapper extends AbstractLDAPStorageMapper implements PasswordUpdated {

    private static final Logger logger = Logger.getLogger(MSADUserAccountControlStorageMapper.class);

    private static final Pattern AUTH_EXCEPTION_REGEX = Pattern.compile(".*AcceptSecurityContext error, data ([0-9a-f]*), v.*");
    private static final Pattern AUTH_INVALID_NEW_PASSWORD = Pattern.compile(".*error code ([0-9a-f]+) .*WILL_NOT_PERFORM.*");

    public MSADUserAccountControlStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider, RealmModel realm) {
        super(mapperModel, ldapProvider, realm);
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
    public void passwordUpdated(UserModel user, LDAPObject ldapUser, CredentialInput input) {
        logger.debugf("Going to update userAccountControl for ldap user '%s' after successful password update", ldapUser.getDn().toString());

        // Normally it's read-only
        ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

        ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "-1");

        UserAccountControl control = getUserAccountControl(ldapUser);
        control.remove(UserAccountControl.PASSWD_NOTREQD);
        control.remove(UserAccountControl.PASSWORD_EXPIRED);

        if (user.isEnabled()) {
            control.remove(UserAccountControl.ACCOUNTDISABLE);
        }

        updateUserAccountControl(ldapUser, control);
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate) {
        return new MSADUserModelDelegate(delegate, ldapUser);
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser) {

    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate) {

    }

    @Override
    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException) {
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
        logger.debugf("MSAD Error code is '%s' after failed LDAP login of user '%s'", errorCode, user.getUsername());

        if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
            if (errorCode.equals("532") || errorCode.equals("773")) {
                // User needs to change his MSAD password. Allow him to login, but add UPDATE_PASSWORD required action
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                return true;
            } else if (errorCode.equals("533")) {
                // User is disabled in MSAD. Set him to disabled in KC as well
                user.setEnabled(false);
                return true;
            } else if (errorCode.equals("775")) {
                logger.warnf("Locked user '%s' attempt to login", user.getUsername());
            }
        }

        return false;
    }


    protected ModelException processFailedPasswordUpdateException(ModelException e) {
        if (e.getCause() == null || e.getCause().getMessage() == null) {
            return e;
        }

        String exceptionMessage = e.getCause().getMessage().replace('\n', ' ');
        Matcher m = AUTH_INVALID_NEW_PASSWORD.matcher(exceptionMessage);
        if (m.matches()) {
            String errorCode = m.group(1);
            if (errorCode.equals("53")) {
                ModelException me = new ModelException("invalidPasswordRegexPatternMessage", e);
                me.setParameters(new Object[]{"passwordConstraintViolation"});
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

    // Update user in LDAP
    protected void updateUserAccountControl(LDAPObject ldapUser, UserAccountControl accountControl) {
        String userAccountControlValue = String.valueOf(accountControl.getValue());
        logger.debugf("Updating userAccountControl of user '%s' to value '%s'", ldapUser.getDn().toString(), userAccountControlValue);

        ldapUser.setSingleAttribute(LDAPConstants.USER_ACCOUNT_CONTROL, userAccountControlValue);
        ldapProvider.getLdapIdentityStore().update(ldapUser);
    }



    public class MSADUserModelDelegate extends UserModelDelegate {

        private final LDAPObject ldapUser;

        public MSADUserModelDelegate(UserModel delegate, LDAPObject ldapUser) {
            super(delegate);
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
                logger.debugf("Going to propagate enabled=%s for ldapUser '%s' to MSAD", enabled, ldapUser.getDn().toString());

                UserAccountControl control = getUserAccountControl(ldapUser);
                if (enabled) {
                    control.remove(UserAccountControl.ACCOUNTDISABLE);
                } else {
                    control.add(UserAccountControl.ACCOUNTDISABLE);
                }

                updateUserAccountControl(ldapUser, control);
            }
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
                logger.debugf("Going to propagate required action UPDATE_PASSWORD to MSAD for ldap user '%s' ", ldapUser.getDn().toString());

                // Normally it's read-only
                ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

                ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "0");
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

                // Don't set pwdLastSet in MSAD when it is new user
                UserAccountControl accountControl = getUserAccountControl(ldapUser);
                if (accountControl.getValue() != 0 && !accountControl.has(UserAccountControl.PASSWD_NOTREQD)) {
                    logger.debugf("Going to remove required action UPDATE_PASSWORD from MSAD for ldap user '%s' ", ldapUser.getDn().toString());

                    // Normally it's read-only
                    ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

                    ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "-1");
                    ldapProvider.getLdapIdentityStore().update(ldapUser);
                }
            }
        }

        @Override
        public Set<String> getRequiredActions() {
            Set<String> requiredActions = super.getRequiredActions();

            if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
                if (getPwdLastSet() == 0 || getUserAccountControl(ldapUser).has(UserAccountControl.PASSWORD_EXPIRED)) {
                    requiredActions = new HashSet<>(requiredActions);
                    requiredActions.add(RequiredAction.UPDATE_PASSWORD.toString());
                    return requiredActions;
                }
            }

            return requiredActions;
        }

        protected long getPwdLastSet() {
            String pwdLastSet = ldapUser.getAttributeAsString(LDAPConstants.PWD_LAST_SET);
            return pwdLastSet == null ? 0 : Long.parseLong(pwdLastSet);
        }


    }

}
