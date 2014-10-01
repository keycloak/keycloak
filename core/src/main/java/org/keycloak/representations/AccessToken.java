package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccessToken extends IDToken {
    public static class Access implements Serializable {
        @JsonProperty("roles")
        protected Set<String> roles;
        @JsonProperty("verify_caller")
        protected Boolean verifyCaller;

        public Access() {
        }

        public Access clone() {
            Access access = new Access();
            access.verifyCaller = verifyCaller;
            if (roles != null) {
                access.roles = new HashSet<String>();
                access.roles.addAll(roles);
            }
            return access;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public Access roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        @JsonIgnore
        public boolean isUserInRole(String role) {
            if (roles == null) return false;
            return roles.contains(role);
        }

        public Access addRole(String role) {
            if (roles == null) roles = new HashSet<String>();
            roles.add(role);
            return this;
        }

        public Boolean getVerifyCaller() {
            return verifyCaller;
        }

        public Access verifyCaller(Boolean required) {
            this.verifyCaller = required;
            return this;
        }
    }

    @JsonProperty("trusted-certs")
    protected Set<String> trustedCertificates;

    @JsonProperty("allowed-origins")
    protected Set<String> allowedOrigins;

    @JsonProperty("realm_access")
    protected Access realmAccess;

    @JsonProperty("resource_access")
    protected Map<String, Access> resourceAccess = new HashMap<String, Access>();

    public Map<String, Access> getResourceAccess() {
        return resourceAccess;
    }

    public void setResourceAccess(Map<String, Access> resourceAccess) {
        this.resourceAccess = resourceAccess;
    }




    /**
     * Does the realm require verifying the caller?
     *
     * @return
     */
    @JsonIgnore
    public boolean isVerifyCaller() {
        if (getRealmAccess() != null && getRealmAccess().getVerifyCaller() != null)
            return getRealmAccess().getVerifyCaller().booleanValue();
        return false;
    }

    /**
     * Does the resource override the requirement of verifying the caller?
     *
     * @param resource
     * @return
     */
    @JsonIgnore
    public boolean isVerifyCaller(String resource) {
        Access access = getResourceAccess(resource);
        if (access != null && access.getVerifyCaller() != null) return access.getVerifyCaller().booleanValue();
        return false;
    }

    @JsonIgnore
    public Access getResourceAccess(String resource) {
        return resourceAccess.get(resource);
    }

    public Access addAccess(String service) {
        Access token = new Access();
        resourceAccess.put(service, token);
        return token;
    }

    @Override
    public AccessToken id(String id) {
        return (AccessToken) super.id(id);
    }

    @Override
    public AccessToken expiration(int expiration) {
        return (AccessToken) super.expiration(expiration);
    }

    @Override
    public AccessToken notBefore(int notBefore) {
        return (AccessToken) super.notBefore(notBefore);
    }


    @Override
    public AccessToken issuedAt(int issuedAt) {
        return (AccessToken) super.issuedAt(issuedAt);
    }

    @Override
    public AccessToken issuer(String issuer) {
        return (AccessToken) super.issuer(issuer);
    }

    @Override
    public AccessToken audience(String audience) {
        return (AccessToken) super.audience(audience);
    }

    @Override
    public AccessToken subject(String subject) {
        return (AccessToken) super.subject(subject);
    }

    @Override
    public AccessToken type(String type) {
        return (AccessToken) super.type(type);
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public Access getRealmAccess() {
        return realmAccess;
    }

    public void setRealmAccess(Access realmAccess) {
        this.realmAccess = realmAccess;
    }

    public Set<String> getTrustedCertificates() {
        return trustedCertificates;
    }

    public void setTrustedCertificates(Set<String> trustedCertificates) {
        this.trustedCertificates = trustedCertificates;
    }

    @Override
    public AccessToken issuedFor(String issuedFor) {
        return (AccessToken)super.issuedFor(issuedFor);
    }


}
