package org.keycloak.models.utils;

import net.iharder.Base64;
import org.jboss.logging.Logger;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.ClaimRepresentation;
import org.keycloak.representations.idm.ClientIdentityProviderMappingRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.keycloak.representations.idm.SocialLinkRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepresentationToModel {

    private static Logger logger = Logger.getLogger(RepresentationToModel.class);

    public static void importRealm(KeycloakSession session, RealmRepresentation rep, RealmModel newRealm) {
        convertDeprecatedSocialProviders(rep);

        newRealm.setName(rep.getRealm());
        if (rep.isEnabled() != null) newRealm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) newRealm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) newRealm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) newRealm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) newRealm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) newRealm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) newRealm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) newRealm.setFailureFactor(rep.getFailureFactor());
        if (rep.isEventsEnabled() != null) newRealm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEventsExpiration() != null) newRealm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) newRealm.setEventsListeners(new HashSet<String>(rep.getEventsListeners()));

        if (rep.getNotBefore() != null) newRealm.setNotBefore(rep.getNotBefore());

        if (rep.getAccessTokenLifespan() != null) newRealm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        else newRealm.setAccessTokenLifespan(300);

        if (rep.getSsoSessionIdleTimeout() != null) newRealm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        else newRealm.setSsoSessionIdleTimeout(1800);
        if (rep.getSsoSessionMaxLifespan() != null) newRealm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        else newRealm.setSsoSessionMaxLifespan(36000);

        if (rep.getAccessCodeLifespan() != null) newRealm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        else newRealm.setAccessCodeLifespan(60);

        if (rep.getAccessCodeLifespanUserAction() != null)
            newRealm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        else newRealm.setAccessCodeLifespanUserAction(300);

        if (rep.getAccessCodeLifespanLogin() != null)
            newRealm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        else newRealm.setAccessCodeLifespanLogin(1800);

        if (rep.getSslRequired() != null) newRealm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.isPasswordCredentialGrantAllowed() != null) newRealm.setPasswordCredentialGrantAllowed(rep.isPasswordCredentialGrantAllowed());
        if (rep.isRegistrationAllowed() != null) newRealm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null)
            newRealm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) newRealm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) newRealm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) newRealm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.getPrivateKey() == null || rep.getPublicKey() == null) {
            KeycloakModelUtils.generateRealmKeys(newRealm);
        } else {
            newRealm.setPrivateKeyPem(rep.getPrivateKey());
            newRealm.setPublicKeyPem(rep.getPublicKey());
        }
        if (rep.getCertificate() == null) {
            KeycloakModelUtils.generateRealmCertificate(newRealm);
        } else {
            newRealm.setCertificatePem(rep.getCertificate());
        }
        if (rep.getCodeSecret() == null) {
            newRealm.setCodeSecret(KeycloakModelUtils.generateCodeSecret());
        } else {
            newRealm.setCodeSecret(rep.getCodeSecret());
        }

        if (rep.getLoginTheme() != null) newRealm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) newRealm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) newRealm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) newRealm.setEmailTheme(rep.getEmailTheme());

        if (rep.getRequiredCredentials() != null) {
            for (String requiredCred : rep.getRequiredCredentials()) {
                addRequiredCredential(newRealm, requiredCred);
            }
        } else {
            addRequiredCredential(newRealm, CredentialRepresentation.PASSWORD);
        }

        if (rep.getPasswordPolicy() != null) newRealm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        importIdentityProviders(rep, newRealm);

        if (rep.getApplications() != null) {
            Map<String, ApplicationModel> appMap = createApplications(rep, newRealm);
        }

        if (rep.getRoles() != null) {
            if (rep.getRoles().getRealm() != null) { // realm roles
                for (RoleRepresentation roleRep : rep.getRoles().getRealm()) {
                    createRole(newRealm, roleRep);
                }
            }
            if (rep.getRoles().getApplication() != null) {
                for (Map.Entry<String, List<RoleRepresentation>> entry : rep.getRoles().getApplication().entrySet()) {
                    ApplicationModel app = newRealm.getApplicationByName(entry.getKey());
                    if (app == null) {
                        throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                    }
                    for (RoleRepresentation roleRep : entry.getValue()) {
                        // Application role may already exists (for example if it is defaultRole)
                        RoleModel role = roleRep.getId()!=null ? app.addRole(roleRep.getId(), roleRep.getName()) : app.addRole(roleRep.getName());
                        role.setDescription(roleRep.getDescription());
                    }
                }
            }
            // now that all roles are created, re-iterate and set up composites
            if (rep.getRoles().getRealm() != null) { // realm roles
                for (RoleRepresentation roleRep : rep.getRoles().getRealm()) {
                    RoleModel role = newRealm.getRole(roleRep.getName());
                    addComposites(role, roleRep, newRealm);
                }
            }
            if (rep.getRoles().getApplication() != null) {
                for (Map.Entry<String, List<RoleRepresentation>> entry : rep.getRoles().getApplication().entrySet()) {
                    ApplicationModel app = newRealm.getApplicationByName(entry.getKey());
                    if (app == null) {
                        throw new RuntimeException("App doesn't exist in role definitions: " + entry.getKey());
                    }
                    for (RoleRepresentation roleRep : entry.getValue()) {
                        RoleModel role = app.getRole(roleRep.getName());
                        addComposites(role, roleRep, newRealm);
                    }
                }
            }
        }

        // Setup realm default roles
        if (rep.getDefaultRoles() != null) {
            for (String roleString : rep.getDefaultRoles()) {
                newRealm.addDefaultRole(roleString.trim());
            }
        }
        // Setup application default roles
        if (rep.getApplications() != null) {
            for (ApplicationRepresentation resourceRep : rep.getApplications()) {
                if (resourceRep.getDefaultRoles() != null) {
                    ApplicationModel appModel = newRealm.getApplicationByName(resourceRep.getName());
                    appModel.updateDefaultRoles(resourceRep.getDefaultRoles());
                }
            }
        }

        if (rep.getOauthClients() != null) {
            createOAuthClients(rep, newRealm);
        }


        // Now that all possible roles and applications are created, create scope mappings

        Map<String, ApplicationModel> appMap = newRealm.getApplicationNameMap();

        if (rep.getApplicationScopeMappings() != null) {

            for (Map.Entry<String, List<ScopeMappingRepresentation>> entry : rep.getApplicationScopeMappings().entrySet()) {
                ApplicationModel app = appMap.get(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find application role mappings for app: " + entry.getKey());
                }
                createApplicationScopeMappings(newRealm, app, entry.getValue());
            }
        }

        if (rep.getScopeMappings() != null) {
            for (ScopeMappingRepresentation scope : rep.getScopeMappings()) {
                ClientModel client = newRealm.findClient(scope.getClient());
                if (client == null) {
                    throw new RuntimeException("Unknown client specification in realm scope mappings");
                }
                for (String roleString : scope.getRoles()) {
                    RoleModel role = newRealm.getRole(roleString.trim());
                    if (role == null) {
                        role = newRealm.addRole(roleString.trim());
                    }
                    client.addScopeMapping(role);
                }

            }
        }

        if (rep.getSmtpServer() != null) {
            newRealm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            newRealm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        } else {
            newRealm.setBrowserSecurityHeaders(BrowserSecurityHeaders.defaultHeaders);
        }

        if (rep.getUserFederationProviders() != null) {
            List<UserFederationProviderModel> providerModels = convertFederationProviders(rep.getUserFederationProviders());
            newRealm.setUserFederationProviders(providerModels);
        }

        // create users and their role mappings and social mappings

        if (rep.getUsers() != null) {
            for (UserRepresentation userRep : rep.getUsers()) {
                UserModel user = createUser(session, newRealm, userRep, appMap);
            }
        }

        if(rep.isInternationalizationEnabled() != null){
            newRealm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if(rep.getSupportedLocales() != null){
            newRealm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if(rep.getDefaultLocale() != null){
            newRealm.setDefaultLocale(rep.getDefaultLocale());
        }
    }

    private static void convertDeprecatedSocialProviders(RealmRepresentation rep) {
        if (rep.isSocial() != null && rep.isSocial() && rep.getSocialProviders() != null && !rep.getSocialProviders().isEmpty() && rep.getIdentityProviders() == null) {
            Boolean updateProfileFirstLogin = rep.isUpdateProfileOnInitialSocialLogin() != null && rep.isUpdateProfileOnInitialSocialLogin();
            if (rep.getSocialProviders() != null) {

                List<IdentityProviderRepresentation> identityProviders = new LinkedList<>();
                for (String k : rep.getSocialProviders().keySet()) {
                    if (k.endsWith(".key")) {
                        String providerId = k.split("\\.")[0];
                        String key = rep.getSocialProviders().get(k);
                        String secret = rep.getSocialProviders().get(k.replace(".key", ".secret"));

                        IdentityProviderRepresentation identityProvider = new IdentityProviderRepresentation();
                        identityProvider.setAlias(providerId);
                        identityProvider.setProviderId(providerId);
                        identityProvider.setEnabled(true);
                        identityProvider.setUpdateProfileFirstLogin(updateProfileFirstLogin);

                        Map<String, String> config = new HashMap<>();
                        config.put("clientId", key);
                        config.put("clientSecret", secret);
                        identityProvider.setConfig(config);

                        identityProviders.add(identityProvider);
                    }
                }
                rep.setIdentityProviders(identityProviders);
            }
        }

        rep.setSocial(null);
        rep.setSocialProviders(null);
        rep.setUpdateProfileOnInitialSocialLogin(false);
    }

    private static void convertDeprecatedSocialProviders(UserRepresentation user) {
        if (user.getSocialLinks() != null && !user.getSocialLinks().isEmpty() && user.getFederatedIdentities() == null) {
            List<FederatedIdentityRepresentation> federatedIdentities = new LinkedList<>();
            for (SocialLinkRepresentation social : user.getSocialLinks()) {
                FederatedIdentityRepresentation federatedIdentity = new FederatedIdentityRepresentation();
                federatedIdentity.setIdentityProvider(social.getSocialProvider());
                federatedIdentity.setUserId(social.getSocialUserId());
                federatedIdentity.setUserName(social.getSocialUsername());
            }
            user.setFederatedIdentities(federatedIdentities);
        }

        user.setSocialLinks(null);
    }

    public static void updateRealm(RealmRepresentation rep, RealmModel realm) {
        if (rep.getRealm() != null) {
            realm.setName(rep.getRealm());
        }
        if (rep.isEnabled() != null) realm.setEnabled(rep.isEnabled());
        if (rep.isBruteForceProtected() != null) realm.setBruteForceProtected(rep.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) realm.setMaxFailureWaitSeconds(rep.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) realm.setMinimumQuickLoginWaitSeconds(rep.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) realm.setWaitIncrementSeconds(rep.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) realm.setQuickLoginCheckMilliSeconds(rep.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) realm.setMaxDeltaTimeSeconds(rep.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) realm.setFailureFactor(rep.getFailureFactor());
        if (rep.isPasswordCredentialGrantAllowed() != null) realm.setPasswordCredentialGrantAllowed(rep.isPasswordCredentialGrantAllowed());
        if (rep.isRegistrationAllowed() != null) realm.setRegistrationAllowed(rep.isRegistrationAllowed());
        if (rep.isRegistrationEmailAsUsername() != null) realm.setRegistrationEmailAsUsername(rep.isRegistrationEmailAsUsername());
        if (rep.isRememberMe() != null) realm.setRememberMe(rep.isRememberMe());
        if (rep.isVerifyEmail() != null) realm.setVerifyEmail(rep.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) realm.setResetPasswordAllowed(rep.isResetPasswordAllowed());
        if (rep.getSslRequired() != null) realm.setSslRequired(SslRequired.valueOf(rep.getSslRequired().toUpperCase()));
        if (rep.getAccessCodeLifespan() != null) realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null) realm.setAccessCodeLifespanUserAction(rep.getAccessCodeLifespanUserAction());
        if (rep.getAccessCodeLifespanLogin() != null) realm.setAccessCodeLifespanLogin(rep.getAccessCodeLifespanLogin());
        if (rep.getNotBefore() != null) realm.setNotBefore(rep.getNotBefore());
        if (rep.getAccessTokenLifespan() != null) realm.setAccessTokenLifespan(rep.getAccessTokenLifespan());
        if (rep.getSsoSessionIdleTimeout() != null) realm.setSsoSessionIdleTimeout(rep.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) realm.setSsoSessionMaxLifespan(rep.getSsoSessionMaxLifespan());
        if (rep.getRequiredCredentials() != null) {
            realm.updateRequiredCredentials(rep.getRequiredCredentials());
        }
        if (rep.getLoginTheme() != null) realm.setLoginTheme(rep.getLoginTheme());
        if (rep.getAccountTheme() != null) realm.setAccountTheme(rep.getAccountTheme());
        if (rep.getAdminTheme() != null) realm.setAdminTheme(rep.getAdminTheme());
        if (rep.getEmailTheme() != null) realm.setEmailTheme(rep.getEmailTheme());
        if (rep.isEventsEnabled() != null) realm.setEventsEnabled(rep.isEventsEnabled());
        if (rep.getEventsExpiration() != null) realm.setEventsExpiration(rep.getEventsExpiration());
        if (rep.getEventsListeners() != null) realm.setEventsListeners(new HashSet<String>(rep.getEventsListeners()));

        if (rep.getPasswordPolicy() != null) realm.setPasswordPolicy(new PasswordPolicy(rep.getPasswordPolicy()));

        if (rep.getDefaultRoles() != null) {
            realm.updateDefaultRoles(rep.getDefaultRoles().toArray(new String[rep.getDefaultRoles().size()]));
        }

        if (rep.getSmtpServer() != null) {
            realm.setSmtpConfig(new HashMap(rep.getSmtpServer()));
        }

        if (rep.getBrowserSecurityHeaders() != null) {
            realm.setBrowserSecurityHeaders(rep.getBrowserSecurityHeaders());
        }

        if (rep.getUserFederationProviders() != null) {
            List<UserFederationProviderModel> providerModels = convertFederationProviders(rep.getUserFederationProviders());
            realm.setUserFederationProviders(providerModels);
        }

        if ("GENERATE".equals(rep.getPublicKey())) {
            KeycloakModelUtils.generateRealmKeys(realm);
        }

        if(rep.isInternationalizationEnabled() != null){
            realm.setInternationalizationEnabled(rep.isInternationalizationEnabled());
        }
        if(rep.getSupportedLocales() != null){
            realm.setSupportedLocales(new HashSet<String>(rep.getSupportedLocales()));
        }
        if(rep.getDefaultLocale() != null){
            realm.setDefaultLocale(rep.getDefaultLocale());
        }
    }

    // Basic realm stuff

    public static void addRequiredCredential(RealmModel newRealm, String requiredCred) {
        newRealm.addRequiredCredential(requiredCred);
    }


    private static List<UserFederationProviderModel> convertFederationProviders(List<UserFederationProviderRepresentation> providers) {
        List<UserFederationProviderModel> result = new ArrayList<UserFederationProviderModel>();

        for (UserFederationProviderRepresentation representation : providers) {
            UserFederationProviderModel model = new UserFederationProviderModel(representation.getId(), representation.getProviderName(),
                    representation.getConfig(), representation.getPriority(), representation.getDisplayName(),
                    representation.getFullSyncPeriod(), representation.getChangedSyncPeriod(), representation.getLastSync());
            result.add(model);
        }
        return result;
    }

    // Roles

    public static void createRole(RealmModel newRealm, RoleRepresentation roleRep) {
        RoleModel role = roleRep.getId()!=null ? newRealm.addRole(roleRep.getId(), roleRep.getName()) : newRealm.addRole(roleRep.getName());
        if (roleRep.getDescription() != null) role.setDescription(roleRep.getDescription());
    }

    private static void addComposites(RoleModel role, RoleRepresentation roleRep, RealmModel realm) {
        if (roleRep.getComposites() == null) return;
        if (roleRep.getComposites().getRealm() != null) {
            for (String roleStr : roleRep.getComposites().getRealm()) {
                RoleModel realmRole = realm.getRole(roleStr);
                if (realmRole == null) throw new RuntimeException("Unable to find composite realm role: " + roleStr);
                role.addCompositeRole(realmRole);
            }
        }
        if (roleRep.getComposites().getApplication() != null) {
            for (Map.Entry<String, List<String>> entry : roleRep.getComposites().getApplication().entrySet()) {
                ApplicationModel app = realm.getApplicationByName(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("App doesn't exist in role definitions: " + roleRep.getName());
                }
                for (String roleStr : entry.getValue()) {
                    RoleModel appRole = app.getRole(roleStr);
                    if (appRole == null) throw new RuntimeException("Unable to find composite app role: " + roleStr);
                    role.addCompositeRole(appRole);
                }

            }

        }

    }

    // APPLICATIONS

    private static Map<String, ApplicationModel> createApplications(RealmRepresentation rep, RealmModel realm) {
        Map<String, ApplicationModel> appMap = new HashMap<String, ApplicationModel>();
        for (ApplicationRepresentation resourceRep : rep.getApplications()) {
            ApplicationModel app = createApplication(realm, resourceRep, false);
            appMap.put(app.getName(), app);
        }
        return appMap;
    }

    /**
     * Does not create scope or role mappings!
     *
     * @param realm
     * @param resourceRep
     * @return
     */
    public static ApplicationModel createApplication(RealmModel realm, ApplicationRepresentation resourceRep, boolean addDefaultRoles) {
        logger.debug("************ CREATE APPLICATION: {0}" + resourceRep.getName());
        ApplicationModel applicationModel = resourceRep.getId()!=null ? realm.addApplication(resourceRep.getId(), resourceRep.getName()) : realm.addApplication(resourceRep.getName());
        if (resourceRep.isEnabled() != null) applicationModel.setEnabled(resourceRep.isEnabled());
        applicationModel.setManagementUrl(resourceRep.getAdminUrl());
        if (resourceRep.isSurrogateAuthRequired() != null)
            applicationModel.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
        applicationModel.setBaseUrl(resourceRep.getBaseUrl());
        if (resourceRep.isBearerOnly() != null) applicationModel.setBearerOnly(resourceRep.isBearerOnly());
        if (resourceRep.isPublicClient() != null) applicationModel.setPublicClient(resourceRep.isPublicClient());
        if (resourceRep.isFrontchannelLogout() != null) applicationModel.setFrontchannelLogout(resourceRep.isFrontchannelLogout());
        if (resourceRep.getProtocol() != null) applicationModel.setProtocol(resourceRep.getProtocol());
        if (resourceRep.isFullScopeAllowed() != null) {
            applicationModel.setFullScopeAllowed(resourceRep.isFullScopeAllowed());
        } else {
            applicationModel.setFullScopeAllowed(true);
        }
        if (resourceRep.getNodeReRegistrationTimeout() != null) {
            applicationModel.setNodeReRegistrationTimeout(resourceRep.getNodeReRegistrationTimeout());
        } else {
            applicationModel.setNodeReRegistrationTimeout(-1);
        }
        applicationModel.updateApplication();

        if (resourceRep.getNotBefore() != null) {
            applicationModel.setNotBefore(resourceRep.getNotBefore());
        }

        applicationModel.setSecret(resourceRep.getSecret());
        if (applicationModel.getSecret() == null) {
            KeycloakModelUtils.generateSecret(applicationModel);
        }

        if (resourceRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : resourceRep.getAttributes().entrySet()) {
                applicationModel.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        if (resourceRep.getRedirectUris() != null) {
            for (String redirectUri : resourceRep.getRedirectUris()) {
                applicationModel.addRedirectUri(redirectUri);
            }
        }
        if (resourceRep.getWebOrigins() != null) {
            for (String webOrigin : resourceRep.getWebOrigins()) {
                logger.debugv("Application: {0} webOrigin: {1}", resourceRep.getName(), webOrigin);
                applicationModel.addWebOrigin(webOrigin);
            }
        } else {
            // add origins from redirect uris
            if (resourceRep.getRedirectUris() != null) {
                Set<String> origins = new HashSet<String>();
                for (String redirectUri : resourceRep.getRedirectUris()) {
                    logger.debugv("add redirect-uri to origin: {0}", redirectUri);
                    if (redirectUri.startsWith("http:")) {
                        URI uri = URI.create(redirectUri);
                        String origin = uri.getScheme() + "://" + uri.getHost();
                        if (uri.getPort() != -1) {
                            origin += ":" + uri.getPort();
                        }
                        logger.debugv("adding default application origin: {0}" , origin);
                        origins.add(origin);
                    }
                }
                if (origins.size() > 0) {
                    applicationModel.setWebOrigins(origins);
                }
            }
        }

        if (resourceRep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : resourceRep.getRegisteredNodes().entrySet()) {
                applicationModel.registerNode(entry.getKey(), entry.getValue());
            }
        }

        if (addDefaultRoles && resourceRep.getDefaultRoles() != null) {
            applicationModel.updateDefaultRoles(resourceRep.getDefaultRoles());
        }

        if (resourceRep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            Set<ProtocolMapperModel> mappers = applicationModel.getProtocolMappers();
            for (ProtocolMapperModel mapper : mappers) applicationModel.removeProtocolMapper(mapper);

            for (ProtocolMapperRepresentation mapper : resourceRep.getProtocolMappers()) {
                applicationModel.addProtocolMapper(toModel(mapper));
            }
        }

        applicationModel.updateIdentityProviders(toModel(resourceRep.getIdentityProviders(), realm));

        return applicationModel;
    }

    public static void updateApplication(ApplicationRepresentation rep, ApplicationModel resource) {
        if (rep.getName() != null) resource.setName(rep.getName());
        if (rep.isEnabled() != null) resource.setEnabled(rep.isEnabled());
        if (rep.isBearerOnly() != null) resource.setBearerOnly(rep.isBearerOnly());
        if (rep.isPublicClient() != null) resource.setPublicClient(rep.isPublicClient());
        if (rep.isFullScopeAllowed() != null) resource.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isFrontchannelLogout() != null) resource.setFrontchannelLogout(rep.isFrontchannelLogout());
        if (rep.getAdminUrl() != null) resource.setManagementUrl(rep.getAdminUrl());
        if (rep.getBaseUrl() != null) resource.setBaseUrl(rep.getBaseUrl());
        if (rep.isSurrogateAuthRequired() != null) resource.setSurrogateAuthRequired(rep.isSurrogateAuthRequired());
        if (rep.getNodeReRegistrationTimeout() != null) resource.setNodeReRegistrationTimeout(rep.getNodeReRegistrationTimeout());
        resource.updateApplication();

        if (rep.getProtocol() != null) resource.setProtocol(rep.getProtocol());
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                resource.setAttribute(entry.getKey(), entry.getValue());
            }
        }


        if (rep.getNotBefore() != null) {
            resource.setNotBefore(rep.getNotBefore());
        }
        if (rep.getDefaultRoles() != null) {
            resource.updateDefaultRoles(rep.getDefaultRoles());
        }

        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            resource.setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            resource.setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getRegisteredNodes() != null) {
            for (Map.Entry<String, Integer> entry : rep.getRegisteredNodes().entrySet()) {
                resource.registerNode(entry.getKey(), entry.getValue());
            }
        }

        updateClientIdentityProviders(rep.getIdentityProviders(), resource);
    }

    public static void setClaims(ClientModel model, ClaimRepresentation rep) {
        long mask = model.getAllowedClaimsMask();
        if (rep.getAddress()) {
            mask |= ClaimMask.ADDRESS;
        } else {
            mask &= ~ClaimMask.ADDRESS;
        }
        if (rep.getEmail()) {
            mask |= ClaimMask.EMAIL;
        } else {
            mask &= ~ClaimMask.EMAIL;
        }
        if (rep.getGender()) {
            mask |= ClaimMask.GENDER;
        } else {
            mask &= ~ClaimMask.GENDER;
        }
        if (rep.getLocale()) {
            mask |= ClaimMask.LOCALE;
        } else {
            mask &= ~ClaimMask.LOCALE;
        }
        if (rep.getName()) {
            mask |= ClaimMask.NAME;
        } else {
            mask &= ~ClaimMask.NAME;
        }
        if (rep.getPhone()) {
            mask |= ClaimMask.PHONE;
        } else {
            mask &= ~ClaimMask.PHONE;
        }
        if (rep.getPicture()) {
            mask |= ClaimMask.PICTURE;
        } else {
            mask &= ~ClaimMask.PICTURE;
        }
        if (rep.getProfile()) {
            mask |= ClaimMask.PROFILE;
        } else {
            mask &= ~ClaimMask.PROFILE;
        }
        if (rep.getUsername()) {
            mask |= ClaimMask.USERNAME;
        } else {
            mask &= ~ClaimMask.USERNAME;
        }
        if (rep.getWebsite()) {
            mask |= ClaimMask.WEBSITE;
        } else {
            mask &= ~ClaimMask.WEBSITE;
        }
        model.setAllowedClaimsMask(mask);
    }

    // OAuth clients

    private static void createOAuthClients(RealmRepresentation realmRep, RealmModel realm) {
        for (OAuthClientRepresentation rep : realmRep.getOauthClients()) {
            createOAuthClient(rep, realm);
        }
    }

    public static OAuthClientModel createOAuthClient(String id, String name, RealmModel realm) {
        OAuthClientModel model = id!=null ? realm.addOAuthClient(id, name) : realm.addOAuthClient(name);
        KeycloakModelUtils.generateSecret(model);
        return model;
    }

    public static OAuthClientModel createOAuthClient(OAuthClientRepresentation rep, RealmModel realm) {
        OAuthClientModel model = createOAuthClient(rep.getId(), rep.getName(), realm);

        model.updateIdentityProviders(toModel(rep.getIdentityProviders(), realm));

        updateOAuthClient(rep, model);
        return model;
    }

    public static void updateOAuthClient(OAuthClientRepresentation rep, OAuthClientModel model) {
        if (rep.getName() != null) model.setClientId(rep.getName());
        if (rep.isEnabled() != null) model.setEnabled(rep.isEnabled());
        if (rep.isPublicClient() != null) model.setPublicClient(rep.isPublicClient());
        if (rep.isFrontchannelLogout() != null) model.setFrontchannelLogout(rep.isFrontchannelLogout());
        if (rep.isFullScopeAllowed() != null) model.setFullScopeAllowed(rep.isFullScopeAllowed());
        if (rep.isDirectGrantsOnly() != null) model.setDirectGrantsOnly(rep.isDirectGrantsOnly());
        if (rep.getClaims() != null) {
            setClaims(model, rep.getClaims());
        }
        if (rep.getNotBefore() != null) {
            model.setNotBefore(rep.getNotBefore());
        }
        if (rep.getSecret() != null) model.setSecret(rep.getSecret());
        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            model.setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            model.setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getNotBefore() != null) {
            model.setNotBefore(rep.getNotBefore());
        }
        if (rep.getProtocol() != null) model.setProtocol(rep.getProtocol());
        if (rep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : rep.getAttributes().entrySet()) {
                model.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        updateClientIdentityProviders(rep.getIdentityProviders(), model);

        if (rep.getProtocolMappers() != null) {
            // first, remove all default/built in mappers
            Set<ProtocolMapperModel> mappers = model.getProtocolMappers();
            for (ProtocolMapperModel mapper : mappers) model.removeProtocolMapper(mapper);

            for (ProtocolMapperRepresentation mapper : rep.getProtocolMappers()) {
                model.addProtocolMapper(toModel(mapper));
            }
        }

    }

    // Scope mappings

    public static void createApplicationScopeMappings(RealmModel realm, ApplicationModel applicationModel, List<ScopeMappingRepresentation> mappings) {
        for (ScopeMappingRepresentation mapping : mappings) {
            ClientModel client = realm.findClient(mapping.getClient());
            if (client == null) {
                throw new RuntimeException("Unknown client specified in application scope mappings");
            }
            for (String roleString : mapping.getRoles()) {
                RoleModel role = applicationModel.getRole(roleString.trim());
                if (role == null) {
                    role = applicationModel.addRole(roleString.trim());
                }
                client.addScopeMapping(role);
            }
        }
    }

    // Users

    public static UserModel createUser(KeycloakSession session, RealmModel newRealm, UserRepresentation userRep, Map<String, ApplicationModel> appMap) {
        convertDeprecatedSocialProviders(userRep);

        // Import users just to user storage. Don't federate
        UserModel user = session.userStorage().addUser(newRealm, userRep.getId(), userRep.getUsername(), false);
        user.setEnabled(userRep.isEnabled());
        user.setEmail(userRep.getEmail());
        user.setFirstName(userRep.getFirstName());
        user.setLastName(userRep.getLastName());
        user.setFederationLink(userRep.getFederationLink());
        user.setTotp(userRep.isTotp());
        if (userRep.getAttributes() != null) {
            for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet()) {
                user.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        if (userRep.getRequiredActions() != null) {
            for (String requiredAction : userRep.getRequiredActions()) {
                user.addRequiredAction(UserModel.RequiredAction.valueOf(requiredAction));
            }
        }
        if (userRep.getCredentials() != null) {
            for (CredentialRepresentation cred : userRep.getCredentials()) {
                updateCredential(user, cred);
            }
        }
        if (userRep.getFederatedIdentities() != null) {
            for (FederatedIdentityRepresentation identity : userRep.getFederatedIdentities()) {
                FederatedIdentityModel mappingModel = new FederatedIdentityModel(identity.getIdentityProvider(), identity.getUserId(), identity.getUserName());
                session.users().addFederatedIdentity(newRealm, user, mappingModel);
            }
        }
        if (userRep.getRealmRoles() != null) {
            for (String roleString : userRep.getRealmRoles()) {
                RoleModel role = newRealm.getRole(roleString.trim());
                if (role == null) {
                    role = newRealm.addRole(roleString.trim());
                }
                user.grantRole(role);
            }
        }
        if (userRep.getApplicationRoles() != null) {
            for (Map.Entry<String, List<String>> entry : userRep.getApplicationRoles().entrySet()) {
                ApplicationModel app = appMap.get(entry.getKey());
                if (app == null) {
                    throw new RuntimeException("Unable to find application role mappings for app: " + entry.getKey());
                }
                createApplicationRoleMappings(app, user, entry.getValue());
            }
        }
        return user;
    }

    // Detect if it is "plain-text" or "hashed" representation and update model according to it
    private static void updateCredential(UserModel user, CredentialRepresentation cred) {
        if (cred.getValue() != null) {
            UserCredentialModel plainTextCred = convertCredential(cred);
            user.updateCredential(plainTextCred);
        } else {
            UserCredentialValueModel hashedCred = new UserCredentialValueModel();
            hashedCred.setType(cred.getType());
            hashedCred.setDevice(cred.getDevice());
            hashedCred.setHashIterations(cred.getHashIterations());
            try {
                if (cred.getSalt() != null) hashedCred.setSalt(Base64.decode(cred.getSalt()));
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            hashedCred.setValue(cred.getHashedSaltedValue());
            user.updateCredentialDirectly(hashedCred);
        }
    }

    public static UserCredentialModel convertCredential(CredentialRepresentation cred) {
        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(cred.getType());
        credential.setValue(cred.getValue());
        return credential;
    }

    // Role mappings

    public static void createApplicationRoleMappings(ApplicationModel applicationModel, UserModel user, List<String> roleNames) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        for (String roleName : roleNames) {
            RoleModel role = applicationModel.getRole(roleName.trim());
            if (role == null) {
                role = applicationModel.addRole(roleName.trim());
            }
            user.grantRole(role);

        }
    }

    private static void importIdentityProviders(RealmRepresentation rep, RealmModel newRealm) {
        if (rep.getIdentityProviders() != null) {
            for (IdentityProviderRepresentation representation : rep.getIdentityProviders()) {
                newRealm.addIdentityProvider(toModel(representation));
            }
        }
    }
    public static IdentityProviderModel toModel(IdentityProviderRepresentation representation) {
        IdentityProviderModel identityProviderModel = new IdentityProviderModel();

        identityProviderModel.setInternalId(representation.getInternalId());
        identityProviderModel.setAlias(representation.getAlias());
        identityProviderModel.setProviderId(representation.getProviderId());
        identityProviderModel.setEnabled(representation.isEnabled());
        identityProviderModel.setUpdateProfileFirstLogin(representation.isUpdateProfileFirstLogin());
        identityProviderModel.setAuthenticateByDefault(representation.isAuthenticateByDefault());
        identityProviderModel.setStoreToken(representation.isStoreToken());
        identityProviderModel.setConfig(representation.getConfig());

        return identityProviderModel;
    }

    public static ProtocolMapperModel toModel(ProtocolMapperRepresentation rep) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId(rep.getId());
        model.setName(rep.getName());
        model.setConsentRequired(rep.isConsentRequired());
        model.setConsentText(rep.getConsentText());
        model.setProtocol(rep.getProtocol());
        model.setProtocolMapper(rep.getProtocolMapper());
        model.setConfig(rep.getConfig());
        return model;
    }

    private static List<ClientIdentityProviderMappingModel> toModel(List<ClientIdentityProviderMappingRepresentation> repIdentityProviders, RealmModel realm) {
        List<ClientIdentityProviderMappingModel> result = new ArrayList<ClientIdentityProviderMappingModel>();

        if (repIdentityProviders != null) {
            for (ClientIdentityProviderMappingRepresentation rep : repIdentityProviders) {
                ClientIdentityProviderMappingModel identityProviderMapping = new ClientIdentityProviderMappingModel();

                identityProviderMapping.setIdentityProvider(rep.getId());
                identityProviderMapping.setRetrieveToken(rep.isRetrieveToken());

                result.add(identityProviderMapping);
            }
        }

        return result;
    }

    private static void updateClientIdentityProviders(List<ClientIdentityProviderMappingRepresentation> identityProviders, ClientModel resource) {
        if (identityProviders != null) {
            List<ClientIdentityProviderMappingModel> result = new ArrayList<ClientIdentityProviderMappingModel>();

            for (ClientIdentityProviderMappingRepresentation mappingRepresentation : identityProviders) {
                ClientIdentityProviderMappingModel identityProviderMapping = new ClientIdentityProviderMappingModel();

                identityProviderMapping.setIdentityProvider(mappingRepresentation.getId());
                identityProviderMapping.setRetrieveToken(mappingRepresentation.isRetrieveToken());

                result.add(identityProviderMapping);
            }

            resource.updateIdentityProviders(result);
        }
    }
}
