package org.keycloak.services.models;

import org.bouncycastle.openssl.PEMWriter;
import org.jboss.resteasy.security.PemUtils;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.relationships.RealmAdminRelationship;
import org.keycloak.services.models.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.relationships.ResourceRelationship;
import org.keycloak.services.models.relationships.ScopeRelationship;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.TOTPCredential;
import org.picketlink.idm.credential.TOTPCredentials;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.credential.X509CertificateCredentials;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Grant;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.Tier;
import org.picketlink.idm.model.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Meant to be a per-request object
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmModel {
    public static final String DEFAULT_REALM = "default";
    public static final String REALM_AGENT_ID = "_realm_";
    public static final String REALM_NAME = "name";
    public static final String REALM_ACCESS_CODE_LIFESPAN = "accessCodeLifespan";
    public static final String REALM_TOKEN_LIFESPAN = "tokenLifespan";
    public static final String REALM_PRIVATE_KEY = "privateKey";
    public static final String REALM_PUBLIC_KEY = "publicKey";
    public static final String REALM_IS_SSL_NOT_REQUIRED = "isSSLNotRequired";
    public static final String REALM_IS_COOKIE_LOGIN_ALLOWED = "isCookieLoginAllowed";
    public static final String REALM_IS_REGISTRATION_ALLOWED = "isRegistrationAllowed";

    protected Realm realm;
    protected Agent realmAgent;
    protected IdentitySession identitySession;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected IdentityManager idm;

    public RealmModel(Realm realm, IdentitySession session) {
        this.realm = realm;
        this.identitySession = session;
        realmAgent = getIdm().getAgent(REALM_AGENT_ID);
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = identitySession.createIdentityManager(realm);
        return idm;
    }

    public void updateRealm() {
        getIdm().update(realmAgent);
    }

    public String getId() {
        return realm.getId();
    }

    public String getName() {
        return (String) realmAgent.getAttribute(REALM_NAME).getValue();
    }

    public void setName(String name) {
        realmAgent.setAttribute(new Attribute<String>(REALM_NAME, name));
    }

    public boolean isEnabled() {
        return realmAgent.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        realmAgent.setEnabled(enabled);
    }

    public boolean isSslNotRequired() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_SSL_NOT_REQUIRED).getValue();
    }

    public void setSslNotRequired(boolean sslNotRequired) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_SSL_NOT_REQUIRED, sslNotRequired));
    }

    public boolean isCookieLoginAllowed() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_COOKIE_LOGIN_ALLOWED).getValue();
    }

    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_COOKIE_LOGIN_ALLOWED, cookieLoginAllowed));
    }

    public boolean isRegistrationAllowed() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_REGISTRATION_ALLOWED).getValue();
    }

    public void setRegistrationAllowed(boolean registrationAllowed) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_REGISTRATION_ALLOWED, registrationAllowed));
    }

    public int getTokenLifespan() {
        return (Integer) realmAgent.getAttribute(REALM_TOKEN_LIFESPAN).getValue();
    }

    public void setTokenLifespan(int tokenLifespan) {
        realmAgent.setAttribute(new Attribute<Integer>(REALM_TOKEN_LIFESPAN, tokenLifespan));
    }

    public int getAccessCodeLifespan() {
        return (Integer) realmAgent.getAttribute(REALM_ACCESS_CODE_LIFESPAN).getValue();
    }

    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realmAgent.setAttribute(new Attribute<Integer>(REALM_ACCESS_CODE_LIFESPAN, accessCodeLifespan));
    }

    public String getPublicKeyPem() {
        return (String) realmAgent.getAttribute(REALM_PUBLIC_KEY).getValue();
    }

    public void setPublicKeyPem(String publicKeyPem) {
        realmAgent.setAttribute(new Attribute<String>(REALM_PUBLIC_KEY, publicKeyPem));
        this.publicKey = null;
    }

    public String getPrivateKeyPem() {
        return (String) realmAgent.getAttribute(REALM_PRIVATE_KEY).getValue();
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        realmAgent.setAttribute(new Attribute<String>(REALM_PRIVATE_KEY, privateKeyPem));
        this.privateKey = null;
    }

    public PublicKey getPublicKey() {
        if (publicKey != null) return publicKey;
        String pem = getPublicKeyPem();
        if (pem != null) {
            try {
                publicKey = PemUtils.decodePublicKey(pem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(publicKey);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        setPublicKeyPem(PemUtils.removeBeginEnd(s));
    }

    public PrivateKey getPrivateKey() {
        if (privateKey != null) return privateKey;
        String pem = getPrivateKeyPem();
        if (pem != null) {
            try {
                privateKey = PemUtils.decodePrivateKey(pem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
        StringWriter writer = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(writer);
        try {
            pemWriter.writeObject(privateKey);
            pemWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String s = writer.toString();
        setPrivateKeyPem(PemUtils.removeBeginEnd(s));
    }

    public List<RequiredCredentialModel> getRequiredCredentials() {
        IdentityManager idm = getIdm();
        Agent realmAgent = idm.getAgent(REALM_AGENT_ID);
        RelationshipQuery<RequiredCredentialRelationship> query = idm.createRelationshipQuery(RequiredCredentialRelationship.class);
        query.setParameter(RequiredCredentialRelationship.REALM_AGENT, realmAgent);
        List<RequiredCredentialRelationship> results = query.getResultList();
        List<RequiredCredentialModel> rtn = new ArrayList<RequiredCredentialModel>();
        for (RequiredCredentialRelationship relationship : results) {
            RequiredCredentialModel model = new RequiredCredentialModel();
            model.setInput(relationship.isInput());
            model.setSecret(relationship.isSecret());
            model.setType(relationship.getCredentialType());
            rtn.add(model);
        }
        return rtn;
    }

    public void addRequiredCredential(RequiredCredentialModel cred) {
        IdentityManager idm = getIdm();
        Agent realmAgent = idm.getAgent(REALM_AGENT_ID);
        RequiredCredentialRelationship relationship = new RequiredCredentialRelationship();
        relationship.setCredentialType(cred.getType());
        relationship.setInput(cred.isInput());
        relationship.setSecret(cred.isSecret());
        relationship.setRealmAgent(realmAgent);
        idm.add(relationship);
    }

    public boolean validatePassword(UserModel user, String password) {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user.getLoginName(), new Password(password));
        getIdm().validateCredentials(creds);
        return creds.getStatus() == Credentials.Status.VALID;
    }

    public boolean validateTOTP(UserModel user, String password, String token) {
        TOTPCredentials creds = new TOTPCredentials();
        creds.setToken(token);
        creds.setUsername(user.getLoginName());
        creds.setPassword(new Password(password));
        getIdm().validateCredentials(creds);
        return creds.getStatus() == Credentials.Status.VALID;
    }

    public void updateCredential(UserModel user, UserCredentialModel cred) {
        IdentityManager idm = getIdm();
        if (cred.getType().equals(RequiredCredentialRepresentation.PASSWORD)) {
            Password password = new Password(cred.getValue());
            idm.updateCredential(user.getUser(), password);
        } else if (cred.getType().equals(RequiredCredentialRepresentation.TOTP)) {
            TOTPCredential totp = new TOTPCredential(cred.getValue());
            idm.updateCredential(user.getUser(), totp);
        } else if (cred.getType().equals(RequiredCredentialRepresentation.CLIENT_CERT)) {
            X509Certificate cert = null;
            try {
                cert = org.keycloak.PemUtils.decodeCertificate(cred.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            X509CertificateCredentials creds = new X509CertificateCredentials(cert);
            idm.updateCredential(user.getUser(), creds);
        }
    }

    public UserModel getUser(String name) {
        User user = getIdm().getUser(name);
        if (user == null) return null;
        return new UserModel(user, getIdm());
    }

    public UserModel addUser(String username) {
        User user = getIdm().getUser(username);
        if (user != null) throw new IllegalStateException("User already exists");
        user = new SimpleUser(username);
        getIdm().add(user);
        return new UserModel(user, getIdm());
    }

    public RoleModel getRole(String name) {
        Role role = getIdm().getRole(name);
        if (role == null) return null;
        return new RoleModel(role, getIdm());
    }

    public RoleModel addRole(String name) {
        Role role = new SimpleRole(name);
        getIdm().add(role);
        return new RoleModel(role, getIdm());
    }

    public List<RoleModel> getRoles() {
        IdentityManager idm = getIdm();
        IdentityQuery<Role> query = idm.createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, realm);
        List<Role> roles = query.getResultList();
        List<RoleModel> roleModels = new ArrayList<RoleModel>();
        for (Role role : roles) {
            roleModels.add(new RoleModel(role, idm));
        }
        return roleModels;
    }


    /**
     * Key name, value resource
     *
     * @return
     */
    public Map<String, ResourceModel> getResourceMap() {
        Map<String, ResourceModel> resourceMap = new HashMap<String, ResourceModel>();
        for (ResourceModel resource : getResources()) {
            resourceMap.put(resource.getName(), resource);
        }
        return resourceMap;
    }

    public List<ResourceModel> getResources() {
        IdentityManager idm = getIdm();
        RelationshipQuery<ResourceRelationship> query = idm.createRelationshipQuery(ResourceRelationship.class);
        query.setParameter(ResourceRelationship.REALM_AGENT, realmAgent);
        List<ResourceRelationship> results = query.getResultList();
        List<ResourceModel> resources = new ArrayList<ResourceModel>();
        for (ResourceRelationship relationship : results) {
            Tier resourceTier = identitySession.findTier(relationship.getResourceId());
            ResourceModel model = new ResourceModel(resourceTier,relationship, this, identitySession);
            resources.add(model);
        }

        return resources;
    }

    public ResourceModel addResource(String name) {
        Tier newTier = identitySession.createTier(RealmManager.generateId());
        IdentityManager idm = getIdm();
        ResourceRelationship relationship = new ResourceRelationship();
        relationship.setResourceName(name);
        relationship.setRealmAgent(realmAgent);
        relationship.setResourceId(newTier.getId());
        relationship.setManagementUrl(""); // Picketlink doesn't like null attribute values
        User resourceUser = new SimpleUser(name);
        idm.add(resourceUser);
        relationship.setResourceUser(resourceUser);
        idm.add(relationship);
        ResourceModel resource = new ResourceModel(newTier, relationship, this, identitySession);
        resource.addRole("*");
        resource.addScope(new UserModel(resourceUser, idm), "*");
        return resource;
    }

    public boolean hasRole(UserModel user, RoleModel role) {
        return getIdm().hasRole(user.getUser(), role.getRole());
    }

    public void grantRole(UserModel user, RoleModel role) {
        getIdm().grantRole(user.getUser(), role.getRole());
    }

    public Set<String> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getIdm().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, user.getUser());
        List<Grant> grants = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(realm.getId())) set.add(grant.getRole().getName());
        }
        return set;
    }

    public void addScope(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = idm.getRole(roleName);
        if (role == null) throw new RuntimeException("role not found");
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(agent.getUser());
        scope.setScope(role);
        idm.add(scope);

    }


    public Set<String> getScope(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getIdm().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, agent.getUser());
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(realm.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }

    public boolean isRealmAdmin(UserModel agent) {
        IdentityManager idm = new RealmManager(identitySession).defaultRealm().getIdm();
        RelationshipQuery<RealmAdminRelationship> query = idm.createRelationshipQuery(RealmAdminRelationship.class);
        query.setParameter(RealmAdminRelationship.REALM, realm.getId());
        query.setParameter(RealmAdminRelationship.ADMIN, agent.getUser());
        List<RealmAdminRelationship> results = query.getResultList();
        return results.size() > 0;
    }

    public void addRealmAdmin(UserModel agent) {
        IdentityManager idm = new RealmManager(identitySession).defaultRealm().getIdm();
        RealmAdminRelationship relationship = new RealmAdminRelationship();
        relationship.setAdmin(agent.getUser());
        relationship.setRealm(realm.getId());
        idm.add(relationship);
    }
}
