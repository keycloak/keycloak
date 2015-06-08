package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationEventAwareProviderFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;

import java.util.List;
import java.util.Map;

import javax.naming.directory.SearchControls;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_3_0_Beta1 {
    public static final ModelVersion VERSION = new ModelVersion("1.3.0.Beta1");


    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            if (realm.getAuthenticationFlows().size() == 0) {
                DefaultAuthenticationFlows.addFlows(realm);
            }

            migrateLDAPProviders(session, realm);
        }

    }

    private void migrateLDAPProviders(KeycloakSession session, RealmModel realm) {
        List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
        for (UserFederationProviderModel fedProvider : federationProviders) {

            if (fedProvider.getProviderName().equals(LDAPConstants.LDAP_PROVIDER)) {
                Map<String, String> config = fedProvider.getConfig();

                // Update config properties for LDAP federation provider
                config.put(LDAPConstants.SEARCH_SCOPE, String.valueOf(SearchControls.SUBTREE_SCOPE));

                String usersDn = config.remove("userDnSuffix");
                config.put(LDAPConstants.USERS_DN, usersDn);

                String rdnLdapAttribute = config.get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
                if (rdnLdapAttribute != null) {
                    if (rdnLdapAttribute.equalsIgnoreCase(LDAPConstants.SAM_ACCOUNT_NAME)) {
                        config.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, LDAPConstants.CN);
                    } else {
                        config.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, rdnLdapAttribute);
                    }
                }

                String uuidAttrName = LDAPConstants.getUuidAttributeName(config.get(LDAPConstants.VENDOR));
                config.put(LDAPConstants.UUID_LDAP_ATTRIBUTE, uuidAttrName);

                realm.updateUserFederationProvider(fedProvider);

                // Create default mappers for LDAP
                UserFederationProviderFactory ldapFactory = (UserFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, LDAPConstants.LDAP_PROVIDER);
                if (ldapFactory != null) {
                    ((UserFederationEventAwareProviderFactory) ldapFactory).onProviderModelCreated(realm, fedProvider);
                }
            }
        }
    }
}
