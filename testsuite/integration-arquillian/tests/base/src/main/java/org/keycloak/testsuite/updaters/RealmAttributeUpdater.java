package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import java.util.HashMap;
import java.util.List;

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

    public RealmAttributeUpdater setRememberMe(Boolean rememberMe) {
        rep.setRememberMe(rememberMe);
        return this;
    }

    public RealmAttributeUpdater setRegistrationEmailAsUsername(Boolean value) {
        rep.setRegistrationEmailAsUsername(value);
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

    public RealmAttributeUpdater setDefaultLocale(String defaultLocale) {
        rep.setDefaultLocale(defaultLocale);
        return this;
    }

    public RealmAttributeUpdater addSupportedLocale(String locale) {
        rep.addSupportedLocales(locale);
        return this;
    }

    public RealmAttributeUpdater setInternationalizationEnabled(Boolean internationalizationEnabled) {
        rep.setInternationalizationEnabled(internationalizationEnabled);
        return this;
    }
}
