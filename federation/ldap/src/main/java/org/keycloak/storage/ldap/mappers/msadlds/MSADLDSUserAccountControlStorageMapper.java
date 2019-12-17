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

package org.keycloak.storage.ldap.mappers.msadlds;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;
import org.keycloak.storage.ldap.mappers.PasswordUpdateCallback;

import javax.naming.AuthenticationException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mapper specific to MSAD LDS. It's able to read the msDS-UserAccountDisabled, msDS-UserPasswordExpired and pwdLastSet attributes and set actions in Keycloak based on that.
 * It's also able to handle exception code from LDAP user authentication (See http://www-01.ibm.com/support/docview.wss?uid=swg21290631 )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:slawomir@dabek.name">Slawomir Dabek</a>
 */
public class MSADLDSUserAccountControlStorageMapper extends AbstractLDAPStorageMapper implements PasswordUpdateCallback {

    private static final Logger logger = Logger.getLogger(MSADLDSUserAccountControlStorageMapper.class);

    private static final Pattern AUTH_EXCEPTION_REGEX = Pattern.compile(".*AcceptSecurityContext error, data ([0-9a-f]*), v.*");
    private static final Pattern AUTH_INVALID_NEW_PASSWORD = Pattern.compile("(?s).*problem 1005 \\(CONSTRAINT_ATT_TYPE\\), data [0-9a-f]*, Att 23 \\(userPassword\\).*");

    public MSADLDSUserAccountControlStorageMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
        ldapProvider.setUpdater(this);
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
        query.addReturningLdapAttribute(LDAPConstants.PWD_LAST_SET);
        query.addReturningLdapAttribute(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED);

        // This needs to be read-only and can be set to writable just on demand
        query.addReturningReadOnlyLdapAttribute(LDAPConstants.PWD_LAST_SET);

        if (ldapProvider.getEditMode() != UserStorageProvider.EditMode.WRITABLE) {
            query.addReturningReadOnlyLdapAttribute(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED);
        }
    }

    @Override
    public LDAPOperationDecorator beforePasswordUpdate(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
        return null; // Not supported for now. Not sure if LDAP_SERVER_POLICY_HINTS_OID works in MSAD LDS
    }

    @Override
    public void passwordUpdated(UserModel user, LDAPObject ldapUser, UserCredentialModel password) {
        logger.debugf("Going to update pwdLastSet for ldap user '%s' after successful password update", ldapUser.getDn().toString());

        // Normally it's read-only
        ldapUser.removeReadOnlyAttributeName(LDAPConstants.PWD_LAST_SET);

        ldapUser.setSingleAttribute(LDAPConstants.PWD_LAST_SET, "-1");
        
        if (user.isEnabled()) {
            // TODO: Use removeAttribute once available
            ldapUser.setSingleAttribute(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED, "FALSE");
            logger.debugf("Removing msDS-UserPasswordExpired of user '%s'", ldapUser.getDn().toString());
        }

        ldapProvider.getLdapIdentityStore().update(ldapUser);
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
        logger.debugf("MSAD LDS Error code is '%s' after failed LDAP login of user '%s'", errorCode, user.getUsername());

        if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE) {
            if (errorCode.equals("532") || errorCode.equals("773")) {
                // User needs to change his MSAD password. Allow him to login, but add UPDATE_PASSWORD required action
                if (!user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_PASSWORD.name())) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                }
                return true;
            } else if (errorCode.equals("533")) {
                // User is disabled in MSAD LDS. Set him to disabled in KC as well
                if (user.isEnabled()) {
                    user.setEnabled(false);
                }
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

        String exceptionMessage = e.getCause().getMessage();
        Matcher m = AUTH_INVALID_NEW_PASSWORD.matcher(exceptionMessage);
        if (m.matches()) {
            ModelException me = new ModelException("invalidPasswordRegexPatternMessage", e);
            me.setParameters(new Object[]{"passwordConstraintViolation"});
            return me;
        }

        return e;
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
                // Merge KC and MSAD LDS
                return kcEnabled && !Boolean.parseBoolean(ldapUser.getAttributeAsString(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED));
            } else {
                // If new MSAD LDS user is created and pwdLastSet is still 0, MSAD account is in disabled state. So read just from Keycloak DB. User is not able to login via MSAD anyway
                return kcEnabled;
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            // Always update DB
            super.setEnabled(enabled);

            if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && getPwdLastSet() > 0) {
                if (enabled) {
                    logger.debugf("Removing msDS-UserAccountDisabled of user '%s'", ldapUser.getDn().toString());
                    // TODO: Use removeAttribute once available
                    ldapUser.setSingleAttribute(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED, "FALSE");
                } else {
                    logger.debugf("Setting msDS-UserAccountDisabled of user '%s' to value 'TRUE'", ldapUser.getDn().toString());
                    ldapUser.setSingleAttribute(LDAPConstants.MSDS_USER_ACCOUNT_DISABLED, "TRUE");
                }
                
                ldapProvider.getLdapIdentityStore().update(ldapUser);
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

                // Don't set pwdLastSet in MSAD LDS when it is new user
                if (!Boolean.parseBoolean(ldapUser.getAttributeAsString(LDAPConstants.MSDS_USER_PASSWORD_NOTREQD))) {
                    logger.debugf("Going to remove required action UPDATE_PASSWORD from MSAD LDS for ldap user '%s' ", ldapUser.getDn().toString());

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
                if (getPwdLastSet() == 0 || Boolean.parseBoolean(ldapUser.getAttributeAsString(LDAPConstants.MSDS_USER_PASSWORD_EXPIRED))) {
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
