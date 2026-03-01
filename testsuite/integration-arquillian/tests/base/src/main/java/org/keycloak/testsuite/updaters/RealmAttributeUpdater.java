package org.keycloak.testsuite.updaters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Updater for realm attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class RealmAttributeUpdater extends ServerResourceUpdater<RealmAttributeUpdater, RealmResource, RealmRepresentation> {

    public RealmAttributeUpdater(RealmResource resource) {
        super(resource, resource::toRepresentation, resource::update);
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public RealmAttributeUpdater setAttribute(String name, String value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public RealmAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public RealmAttributeUpdater setPublicKey(String key) {
        this.rep.setPublicKey(key);
        return this;
    }

    public RealmAttributeUpdater setPrivateKey(String key) {
        this.rep.setPrivateKey(key);
        return this;
    }

    public RealmAttributeUpdater setDefaultDefaultClientScopes(List<String> defaultClientScopes) {
        rep.setDefaultDefaultClientScopes(defaultClientScopes);
        return this;
    }
    
    public RealmAttributeUpdater setAccessCodeLifespan(Integer accessCodeLifespan) {
        rep.setAccessCodeLifespan(accessCodeLifespan);
        return this;
    }

    public RealmAttributeUpdater setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin) {
        rep.setAccessCodeLifespanLogin(accessCodeLifespanLogin);
        return this;
    }

    public RealmAttributeUpdater setSsoSessionIdleTimeout(Integer timeout) {
        rep.setSsoSessionIdleTimeout(timeout);
        return this;
    }

    public RealmAttributeUpdater setSsoSessionMaxLifespan(Integer timeout) {
        rep.setSsoSessionMaxLifespan(timeout);
        return this;
    }

    public RealmAttributeUpdater setSsoSessionIdleTimeoutRememberMe(Integer idleTimeout) {
        rep.setSsoSessionIdleTimeoutRememberMe(idleTimeout);
        return this;
    }

    public RealmAttributeUpdater setSsoSessionMaxLifespanRememberMe(Integer maxLifespan) {
        rep.setSsoSessionMaxLifespanRememberMe(maxLifespan);
        return this;
    }

    public RealmAttributeUpdater setAccessTokenLifespanForImplicitFlow(Integer lifespan) {
        rep.setAccessTokenLifespanForImplicitFlow(lifespan);
        return this;
    }

    public RealmAttributeUpdater setRememberMe(Boolean rememberMe) {
        rep.setRememberMe(rememberMe);
        return this;
    }

    public RealmAttributeUpdater setRegistrationEmailAsUsername(Boolean value) {
        rep.setRegistrationEmailAsUsername(value);
        return this;
    }

    public RealmAttributeUpdater setEditUserNameAllowed(Boolean value) {
        rep.setEditUsernameAllowed(value);
        return this;
    }

    public RealmAttributeUpdater setPermanentLockout(Boolean value) {
        rep.setPermanentLockout(value);
        return this;
    }

    public RealmAttributeUpdater setQuickLoginCheckMilliSeconds(Long value) {
        rep.setQuickLoginCheckMilliSeconds(value);
        return this;
    }

    public RealmAttributeUpdater setWaitIncrementSeconds(Integer value) {
        rep.setWaitIncrementSeconds(value);
        return this;
    }

    public RealmAttributeUpdater setMaxFailureWaitSeconds(Integer value) {
        rep.setMaxFailureWaitSeconds(value);
        return this;
    }

    public RealmAttributeUpdater setMaxDeltaTimeSeconds(Integer value) {
        rep.setMaxDeltaTimeSeconds(value);
        return this;
    }

    public RealmAttributeUpdater setEventsListeners(List<String> eventListanets) {
        rep.setEventsListeners(eventListanets);
        return this;
    }

    public RealmAttributeUpdater addEventsListener(String value) {
        List<String> list = new ArrayList<>(rep.getEventsListeners());
        list.add(value);
        rep.setEventsListeners(list);
        return this;
    }

    public RealmAttributeUpdater setDuplicateEmailsAllowed(Boolean value) {
        rep.setDuplicateEmailsAllowed(value);
        return this;
    }

    public RealmAttributeUpdater setPasswordPolicy(String policy) {
        rep.setPasswordPolicy(policy);
        return this;
    }

    public RealmAttributeUpdater setVerifyEmail(Boolean value) {
        rep.setVerifyEmail(value);
        return this;
    }

    public RealmAttributeUpdater setBrowserFlow(String browserFlow) {
        rep.setBrowserFlow(browserFlow);
        return this;
    }

    public RealmAttributeUpdater setNotBefore(Integer notBefore) {
        rep.setNotBefore(notBefore);
        return this;
    }

    public RealmAttributeUpdater setDefaultLocale(String defaultLocale) {
        rep.setDefaultLocale(defaultLocale);
        return this;
    }

    public RealmAttributeUpdater addSupportedLocale(String locale) {
        if (origRep.getSupportedLocales() == null) {
            origRep.setSupportedLocales(Collections.emptySet());
        }
        rep.addSupportedLocales(locale);
        return this;
    }

    public RealmAttributeUpdater setInternationalizationEnabled(Boolean internationalizationEnabled) {
        rep.setInternationalizationEnabled(internationalizationEnabled);
        return this;
    }

    // OTP Policy
    public RealmAttributeUpdater setOtpPolicyAlgorithm(String otpPolicyAlgorithm) {
        rep.setOtpPolicyAlgorithm(otpPolicyAlgorithm);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyDigits(Integer otpPolicyDigits) {
        rep.setOtpPolicyDigits(otpPolicyDigits);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyInitialCounter(Integer otpPolicyInitialCounter) {
        rep.setOtpPolicyInitialCounter(otpPolicyInitialCounter);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyPeriod(Integer otpPolicyPeriod) {
        rep.setOtpPolicyPeriod(otpPolicyPeriod);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyType(String otpPolicyType) {
        rep.setOtpPolicyType(otpPolicyType);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyLookAheadWindow(Integer otpPolicyLookAheadWindow) {
        rep.setOtpPolicyLookAheadWindow(otpPolicyLookAheadWindow);
        return this;
    }

    public RealmAttributeUpdater setOtpPolicyCodeReusable(Boolean isCodeReusable) {
        rep.setOtpPolicyCodeReusable(isCodeReusable);
        return this;
    }

    public RealmAttributeUpdater setSmtpServer(String name, String value) {
        rep.getSmtpServer().put(name, value);
        return this;
    }

    public RealmAttributeUpdater setBrowserSecurityHeader(String name, String value) {
        rep.getBrowserSecurityHeaders().put(name, value);
        return this;
    }

    public RealmAttributeUpdater setOrganizationsEnabled(Boolean organizationsEnabled) {
        rep.setOrganizationsEnabled(organizationsEnabled);
        return this;
    }

    public RealmAttributeUpdater setRegistrationAllowed(Boolean registrationAllowed) {
        rep.setRegistrationAllowed(registrationAllowed);
        return this;
    }

    public RealmAttributeUpdater setAdminPermissionsEnabled(Boolean adminPermissionsEnabled) {
        rep.setAdminPermissionsEnabled(adminPermissionsEnabled);
        return this;
    }

    public RealmAttributeUpdater setWebAuthnPolicyPasswordlessPasskeysEnabled(Boolean passkeysEnabled) {
        rep.setWebAuthnPolicyPasswordlessPasskeysEnabled(passkeysEnabled);
        return this;
    }
}
