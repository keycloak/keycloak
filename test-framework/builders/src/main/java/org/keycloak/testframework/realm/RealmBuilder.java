package org.keycloak.testframework.realm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class RealmBuilder extends Builder<RealmRepresentation> {

    private RealmBuilder(RealmRepresentation rep) {
        super(rep);
    }

    public static RealmBuilder create() {
        return new RealmBuilder(new RealmRepresentation()).enabled(true);
    }

    public static RealmBuilder update(RealmRepresentation rep) {
        return new RealmBuilder(rep);
    }

    public RealmBuilder enabled(boolean enabled) {
        rep.setEnabled(enabled);
        return this;
    }

    public RealmBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RealmBuilder name(String name) {
        rep.setRealm(name);
        return this;
    }

    public RealmBuilder displayName(String displayName) {
        rep.setDisplayName(displayName);
        return this;
    }

    @Deprecated
    public RealmBuilder publicKey(String publicKey) {
        rep.setPublicKey(publicKey);
        return this;
    }

    @Deprecated
    public RealmBuilder privateKey(String privateKey) {
        rep.setPrivateKey(privateKey);
        return this;
    }

    public RealmBuilder clients(ClientRepresentation... clients) {
        rep.setClients(combine(rep.getClients(), clients));
        return this;
    }

    public RealmBuilder clients(ClientBuilder... clients) {
        rep.setClients(combine(rep.getClients(), clients));
        return this;
    }

    public RealmBuilder users(UserRepresentation... users) {
        rep.setUsers(combine(rep.getUsers(), users));
        return this;
    }

    public RealmBuilder users(UserBuilder... users) {
        rep.setUsers(combine(rep.getUsers(), users));
        return this;
    }

    public RealmBuilder groups(String... groups) {
        rep.setGroups(combine(GroupBuilder::create, rep.getGroups(), groups));
        return this;
    }

    public RealmBuilder groups(GroupBuilder... groups) {
        rep.setGroups(combine(rep.getGroups(), groups));
        return this;
    }

    public RealmBuilder defaultGroups(String... groupsNames) {
        rep.setDefaultGroups(combine(rep.getDefaultGroups(), groupsNames));
        return this;
    }

    public RealmBuilder roles(String... roleNames) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setRealm(combine(RoleBuilder::create, rep.getRoles().getRealm(), roleNames));
        return this;
    }

    public RealmBuilder realmRoles(String... realmRoles) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setRealm(combine(RoleBuilder::create, rep.getRoles().getRealm(), realmRoles));
        return this;
    }

    public RealmBuilder realmRoles(RoleBuilder... realmRoles) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setRealm(combine(rep.getRoles().getRealm(), realmRoles));
        return this;
    }

    public RealmBuilder realmRoles(RoleRepresentation... realmRoles) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setRealm(combine(rep.getRoles().getRealm(), realmRoles));
        return this;
    }

    public RealmBuilder clientRoles(String client, String... clientRoles) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setClient(combine(RoleBuilder::create, rep.getRoles().getClient(), client, clientRoles));
        return this;
    }

    public RealmBuilder clientRoles(String client, RoleBuilder... clientRoles) {
        rep.setRoles(createIfNull(rep.getRoles(), RolesRepresentation::new));
        rep.getRoles().setClient(combine(rep.getRoles().getClient(), client, clientRoles));
        return this;
    }

    public RealmBuilder authenticationFlows(AuthenticationFlowBuilder... authenticationFlows) {
        rep.setAuthenticationFlows(combine(rep.getAuthenticationFlows(), authenticationFlows));
        return this;
    }

    public RealmBuilder identityProviders(IdentityProviderRepresentation... identityProviders) {
        rep.setIdentityProviders(combine(rep.getIdentityProviders(), identityProviders));
        return this;
    }

    public RealmBuilder identityProviders(IdentityProviderBuilder... identityProviders) {
        rep.setIdentityProviders(combine(rep.getIdentityProviders(), identityProviders));
        return this;
    }

    public RealmBuilder identityProviderMappers(IdentityProviderMapperRepresentation... identityProviderMappers) {
        rep.setIdentityProviderMappers(combine(rep.getIdentityProviderMappers(), identityProviderMappers));
        return this;
    }

    public RealmBuilder registrationEmailAsUsername(boolean registrationEmailAsUsername) {
        rep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
        return this;
    }

    public RealmBuilder registrationAllowed(boolean allowed) {
        rep.setRegistrationAllowed(allowed);
        return this;
    }

    public RealmBuilder verifyEmail(boolean verifyEmail) {
        rep.setVerifyEmail(verifyEmail);
        return this;
    }

    public RealmBuilder editUsernameAllowed(boolean allowed) {
        rep.setEditUsernameAllowed(allowed);
        return this;
    }

    public RealmBuilder defaultSignatureAlgorithm(String algorithm) {
        rep.setDefaultSignatureAlgorithm(algorithm);
        return this;
    }

    public RealmBuilder adminPermissionsEnabled(boolean enabled) {
        rep.setAdminPermissionsEnabled(enabled);
        return this;
    }

    public RealmBuilder eventsEnabled(boolean enabled) {
        rep.setEventsEnabled(enabled);
        return this;
    }

    public RealmBuilder adminEventsEnabled(boolean enabled) {
        rep.setAdminEventsEnabled(enabled);
        return this;
    }

    public RealmBuilder adminEventsDetailsEnabled(boolean enabled) {
        rep.setAdminEventsDetailsEnabled(enabled);
        return this;
    }

    public RealmBuilder enabledEventTypes(String... enabledEventTypes) {
        rep.setEnabledEventTypes(combine(rep.getEnabledEventTypes(), enabledEventTypes));
        return this;
    }

    public RealmBuilder setEnabledEventTypes(List<String> enabledEventTypes) {
        rep.setEnabledEventTypes(enabledEventTypes);
        return this;
    }

    public RealmBuilder eventsListeners(String... eventListeners) {
        rep.setEventsListeners(combine(rep.getEventsListeners(), eventListeners));
        return this;
    }

    public RealmBuilder removeEventListeners(String... eventListeners) {
        rep.setEventsListeners(removeValues(rep.getEventsListeners(), eventListeners));
        return this;
    }

    public RealmBuilder setEventsListeners(List<String> eventListeners) {
        rep.setEventsListeners(eventListeners);
        return this;
    }

    public RealmBuilder eventsExpiration(long eventsExpiration) {
        rep.setEventsExpiration(eventsExpiration);
        return this;
    }

    public RealmBuilder internationalizationEnabled(boolean enabled) {
        rep.setInternationalizationEnabled(enabled);
        return this;
    }

    public RealmBuilder supportedLocales(String... supportedLocales) {
        rep.setSupportedLocales(combine(rep.getSupportedLocales(), supportedLocales));
        return this;
    }

    public RealmBuilder defaultLocale(String locale) {
        rep.setDefaultLocale(locale);
        return this;
    }

    public RealmBuilder smtp(String host, int port, String from) {
        Map<String, String> config = new HashMap<>();
        config.put("host", host);
        config.put("port", Integer.toString(port));
        config.put("from", from);
        rep.setSmtpServer(config);
        return this;
    }

    public RealmBuilder organizationsEnabled(boolean enabled) {
        rep.setOrganizationsEnabled(enabled);
        return this;
    }

    public RealmBuilder revokeRefreshToken(boolean enabled) {
        rep.setRevokeRefreshToken(enabled);
        return this;
    }

    public RealmBuilder refreshTokenMaxReuse(Integer refreshTokenMaxReuse) {
        rep.setRefreshTokenMaxReuse(refreshTokenMaxReuse);
        return this;
    }

    public RealmBuilder accessTokenLifespan(int accessTokenLifespan) {
        rep.setAccessTokenLifespan(accessTokenLifespan);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeout(Integer ssoSessionIdleTimeout) {
        rep.setSsoSessionIdleTimeout(ssoSessionIdleTimeout);
        return this;
    }

    public RealmBuilder ssoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe) {
        rep.setSsoSessionIdleTimeoutRememberMe(ssoSessionIdleTimeoutRememberMe);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespan(Integer ssoSessionMaxLifespan) {
        rep.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
        return this;
    }

    public RealmBuilder ssoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe) {
        rep.setSsoSessionMaxLifespanRememberMe(ssoSessionMaxLifespanRememberMe);
        return this;
    }

    public RealmBuilder clientSessionMaxLifespan(Integer clientSessionMaxLifespan) {
        rep.setClientSessionMaxLifespan(clientSessionMaxLifespan);
        return this;
    }

    public RealmBuilder offlineSessionIdleTimeout(int offlineSessionIdleTimeout) {
        rep.setOfflineSessionIdleTimeout(offlineSessionIdleTimeout);
        return this;
    }

    public RealmBuilder offlineSessionMaxLifespan(int offlineSessionMaxLifespan) {
        rep.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan);
        return this;
    }

    public RealmBuilder offlineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        rep.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
        return this;
    }

    public RealmBuilder accessCodeLifespan(int accessCodeLifespan) {
        rep.setAccessCodeLifespan(accessCodeLifespan);
        return this;
    }

    public RealmBuilder accessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        rep.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
        return this;
    }

    public RealmBuilder clientSessionIdleTimeout(Integer clientSessionIdleTimeout) {
        rep.setClientSessionIdleTimeout(clientSessionIdleTimeout);
        return this;
    }

    public RealmBuilder clientSessionMaxLifespan(int clientSessionMaxLifespan) {
        rep.setClientSessionMaxLifespan(clientSessionMaxLifespan);
        return this;
    }

    public RealmBuilder clientOfflineSessionIdleTimeout(int clientOfflineSessionIdleTimeout) {
        rep.setClientOfflineSessionIdleTimeout(clientOfflineSessionIdleTimeout);
        return this;
    }

    public RealmBuilder clientOfflineSessionMaxLifespan(int clientOfflineSessionMaxLifespan) {
        rep.setClientOfflineSessionMaxLifespan(clientOfflineSessionMaxLifespan);
        return this;
    }

    public RealmBuilder notBefore(int i) {
        rep.setNotBefore(i);
        return this;
    }

    public RealmBuilder otpDigits(int i) {
        rep.setOtpPolicyDigits(i);
        return this;
    }

    public RealmBuilder otpPeriod(int i) {
        rep.setOtpPolicyPeriod(i);
        return this;
    }

    public RealmBuilder otpType(String type) {
        rep.setOtpPolicyType(type);
        return this;
    }

    public RealmBuilder otpAlgorithm(String algorithm) {
        rep.setOtpPolicyAlgorithm(algorithm);
        return this;
    }

    public RealmBuilder otpInitialCounter(int i) {
        rep.setOtpPolicyInitialCounter(i);
        return this;
    }

    public RealmBuilder otpLookAheadWindow(int i) {
        rep.setOtpPolicyLookAheadWindow(i);
        return this;
    }

    public RealmBuilder passwordPolicy(String passwordPolicy) {
        rep.setPasswordPolicy(passwordPolicy);
        return this;
    }

    public RealmBuilder bruteForceProtected(boolean enabled) {
        rep.setBruteForceProtected(enabled);
        return this;
    }

    public RealmBuilder failureFactor(int count) {
        rep.setFailureFactor(count);
        return this;
    }

    public RealmBuilder duplicateEmailsAllowed(boolean allowed) {
        rep.setDuplicateEmailsAllowed(allowed);
        return this;
    }

    public RealmBuilder sslRequired(String sslRequired) {
        rep.setSslRequired(sslRequired);
        return this;
    }

    public RealmBuilder setRememberMe(boolean enabled) {
        rep.setRememberMe(enabled);
        return this;
    }

    public RealmBuilder resetPasswordAllowed(boolean allowed) {
        rep.setResetPasswordAllowed(allowed);
        return this;
    }

    public RealmBuilder resetClientPolicies() {
        rep.setParsedClientPolicies(null);
        return this;
    }

    public RealmBuilder clientPolicy(ClientPolicyRepresentation clientPolicyRep) {
        ClientPoliciesRepresentation clientPolicies = rep.getParsedClientPolicies();
        if (clientPolicies == null) {
            clientPolicies = new ClientPoliciesRepresentation();
        }
        List<ClientPolicyRepresentation> policies = clientPolicies.getPolicies();
        policies.add(clientPolicyRep);
        rep.setParsedClientPolicies(clientPolicies);
        return this;
    }

    public RealmBuilder resetClientProfiles() {
        rep.setParsedClientProfiles(null);
        return this;
    }

    public RealmBuilder clientProfile(ClientProfileRepresentation clientProfileRep) {
        ClientProfilesRepresentation clientProfiles = rep.getParsedClientProfiles();
        if (clientProfiles == null) {
            clientProfiles = new ClientProfilesRepresentation();
        }
        List<ClientProfileRepresentation> profiles = clientProfiles.getProfiles();
        profiles.add(clientProfileRep);
        rep.setParsedClientProfiles(clientProfiles);
        return this;
    }

    public RealmBuilder browserFlow(String browserFlow) {
        rep.setBrowserFlow(browserFlow);
        return this;
    }

    public RealmBuilder requiredActions(RequiredActionProviderRepresentation... requiredActions) {
        rep.setRequiredActions(combine(rep.getRequiredActions(), requiredActions));
        return this;
    }

    public RealmBuilder verifiableCredentialsEnabled(boolean enabled) {
        rep.setVerifiableCredentialsEnabled(enabled);
        return this;
    }

    public RealmBuilder webAuthnPolicySignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmBuilder webAuthnPolicyAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyAttestationConveyancePreference(preference);
        return this;
    }

    public RealmBuilder webAuthnPolicyAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmBuilder webAuthnPolicyRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyRequireResidentKey(residentKey);
        return this;
    }

    public RealmBuilder webAuthnPolicyUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyUserVerificationRequirement(requirement);
        return this;
    }

    public RealmBuilder webAuthnPolicyRpEntityName(String entityName) {
        rep.setWebAuthnPolicyRpEntityName(entityName);
        return this;
    }

    public RealmBuilder webAuthnPolicyRpId(String rpId) {
        rep.setWebAuthnPolicyRpId(rpId);
        return this;
    }

    public RealmBuilder webAuthnPolicyCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyCreateTimeout(timeout);
        return this;
    }

    public RealmBuilder webAuthnPolicyAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessSignatureAlgorithms(List<String> algorithms) {
        rep.setWebAuthnPolicySignatureAlgorithms(algorithms);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAttestationConveyancePreference(String preference) {
        rep.setWebAuthnPolicyPasswordlessAttestationConveyancePreference(preference);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAuthenticatorAttachment(String attachment) {
        rep.setWebAuthnPolicyPasswordlessAuthenticatorAttachment(attachment);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessRequireResidentKey(String residentKey) {
        rep.setWebAuthnPolicyPasswordlessRequireResidentKey(residentKey);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessUserVerificationRequirement(String requirement) {
        rep.setWebAuthnPolicyPasswordlessUserVerificationRequirement(requirement);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessRpEntityName(String entityName) {
        rep.setWebAuthnPolicyPasswordlessRpEntityName(entityName);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessCreateTimeout(Integer timeout) {
        rep.setWebAuthnPolicyPasswordlessCreateTimeout(timeout);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(Boolean register) {
        rep.setWebAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister(register);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessPasskeysEnabled(Boolean enabled) {
        rep.setWebAuthnPolicyPasswordlessPasskeysEnabled(enabled);
        return this;
    }

    public RealmBuilder webAuthnPolicyPasswordlessMediation(String mediation) {
        rep.setWebAuthnPolicyPasswordlessMediation(mediation);
        return this;
    }

    public RealmBuilder webAuthnPolicyAcceptableAaguids(List<String> aaguids) {
        rep.setWebAuthnPolicyAcceptableAaguids(aaguids);
        return this;
    }

    public RealmBuilder scimEnabled(boolean enabled) {
        rep.setScimApiEnabled(enabled);
        return this;
    }

    public RealmBuilder attribute(String key, String value) {
        rep.setAttributes(createIfNull(rep.getAttributes(), HashMap::new));
        rep.getAttributes().put(key, value);
        return this;
    }

    public RealmBuilder attributes(RealmAttributesBuilder attributes) {
        combineMap(rep.getAttributes(), attributes.build());
        return this;
    }

    public RealmBuilder clientScopes(ClientScopeRepresentation... clientScopes) {
        rep.setClientScopes(combine(rep.getClientScopes(), clientScopes));
        return this;
    }

    public RealmBuilder clientScopes(ClientScopeBuilder... clientScopes) {
        rep.setClientScopes(combine(rep.getClientScopes(), clientScopes));
        return this;
    }

    /**
     * Best practice is to use other convenience methods when configuring a realm, but while the framework is under
     * active development there may not be a way to perform all updates required. In these cases this method allows
     * applying any changes to the underlying representation.
     *
     * @param update
     * @return this
     * @deprecated
     */
    public RealmBuilder update(RealmUpdate... update) {
        Arrays.stream(update).forEach(u -> u.update(rep));
        return this;
    }

    public RealmRepresentation build() {
        return rep;
    }

    public interface RealmUpdate {

        void update(RealmRepresentation realm);

    }

}
