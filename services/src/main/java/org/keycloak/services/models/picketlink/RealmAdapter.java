package org.keycloak.services.models.picketlink;

import org.bouncycastle.openssl.PEMWriter;
import org.jboss.resteasy.security.PemUtils;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.picketlink.relationships.RealmAdminRelationship;
import org.keycloak.services.models.picketlink.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.picketlink.relationships.ResourceRelationship;
import org.keycloak.services.models.picketlink.relationships.ScopeRelationship;
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
public class RealmAdapter implements RealmModel {
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

    public RealmAdapter(Realm realm, IdentitySession session) {
        this.realm = realm;
        this.identitySession = session;
        realmAgent = getIdm().getAgent(REALM_AGENT_ID);
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = identitySession.createIdentityManager(realm);
        return idm;
    }

    protected void updateRealm() {
        getIdm().update(realmAgent);
    }

    @Override
    public String getId() {
        return realm.getId();
    }

    @Override
    public String getName() {
        return (String) realmAgent.getAttribute(REALM_NAME).getValue();
    }

    @Override
    public void setName(String name) {
        realmAgent.setAttribute(new Attribute<String>(REALM_NAME, name));
        updateRealm();
    }

    @Override
    public boolean isEnabled() {
        return realmAgent.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realmAgent.setEnabled(enabled);
        updateRealm();
    }

    @Override
    public boolean isSslNotRequired() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_SSL_NOT_REQUIRED).getValue();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_SSL_NOT_REQUIRED, sslNotRequired));
        updateRealm();
    }

    @Override
    public boolean isCookieLoginAllowed() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_COOKIE_LOGIN_ALLOWED).getValue();
    }

    @Override
    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_COOKIE_LOGIN_ALLOWED, cookieLoginAllowed));
        updateRealm();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return (Boolean) realmAgent.getAttribute(REALM_IS_REGISTRATION_ALLOWED).getValue();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realmAgent.setAttribute(new Attribute<Boolean>(REALM_IS_REGISTRATION_ALLOWED, registrationAllowed));
        updateRealm();
    }

    @Override
    public int getTokenLifespan() {
        return (Integer) realmAgent.getAttribute(REALM_TOKEN_LIFESPAN).getValue();
    }

    @Override
    public void setTokenLifespan(int tokenLifespan) {
        realmAgent.setAttribute(new Attribute<Integer>(REALM_TOKEN_LIFESPAN, tokenLifespan));
        updateRealm();
    }

    @Override
    public int getAccessCodeLifespan() {
        return (Integer) realmAgent.getAttribute(REALM_ACCESS_CODE_LIFESPAN).getValue();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realmAgent.setAttribute(new Attribute<Integer>(REALM_ACCESS_CODE_LIFESPAN, accessCodeLifespan));
        updateRealm();
    }

    @Override
    public String getPublicKeyPem() {
        return (String) realmAgent.getAttribute(REALM_PUBLIC_KEY).getValue();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realmAgent.setAttribute(new Attribute<String>(REALM_PUBLIC_KEY, publicKeyPem));
        this.publicKey = null;
        updateRealm();
    }

    @Override
    public String getPrivateKeyPem() {
        return (String) realmAgent.getAttribute(REALM_PRIVATE_KEY).getValue();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        realmAgent.setAttribute(new Attribute<String>(REALM_PRIVATE_KEY, privateKeyPem));
        this.privateKey = null;
        updateRealm();
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public boolean validatePassword(UserModel user, String password) {
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user.getLoginName(), new Password(password));
        getIdm().validateCredentials(creds);
        return creds.getStatus() == Credentials.Status.VALID;
    }

    @Override
    public boolean validateTOTP(UserModel user, String password, String token) {
        TOTPCredentials creds = new TOTPCredentials();
        creds.setToken(token);
        creds.setUsername(user.getLoginName());
        creds.setPassword(new Password(password));
        getIdm().validateCredentials(creds);
        return creds.getStatus() == Credentials.Status.VALID;
    }

    @Override
    public void updateCredential(UserModel user, UserCredentialModel cred) {
        IdentityManager idm = getIdm();
        if (cred.getType().equals(RequiredCredentialRepresentation.PASSWORD)) {
            Password password = new Password(cred.getValue());
            idm.updateCredential(((UserAdapter)user).getUser(), password);
        } else if (cred.getType().equals(RequiredCredentialRepresentation.TOTP)) {
            TOTPCredential totp = new TOTPCredential(cred.getValue());
            idm.updateCredential(((UserAdapter)user).getUser(), totp);
        } else if (cred.getType().equals(RequiredCredentialRepresentation.CLIENT_CERT)) {
            X509Certificate cert = null;
            try {
                cert = org.keycloak.PemUtils.decodeCertificate(cred.getValue());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            X509CertificateCredentials creds = new X509CertificateCredentials(cert);
            idm.updateCredential(((UserAdapter)user).getUser(), creds);
        }
    }

    @Override
    public UserAdapter getUser(String name) {
        User user = getIdm().getUser(name);
        if (user == null) return null;
        return new UserAdapter(user, getIdm());
    }

    @Override
    public UserAdapter addUser(String username) {
        User user = getIdm().getUser(username);
        if (user != null) throw new IllegalStateException("User already exists");
        user = new SimpleUser(username);
        getIdm().add(user);
        return new UserAdapter(user, getIdm());
    }

    @Override
    public RoleAdapter getRole(String name) {
        Role role = getIdm().getRole(name);
        if (role == null) return null;
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public RoleAdapter addRole(String name) {
        Role role = new SimpleRole(name);
        getIdm().add(role);
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public List<RoleModel> getRoles() {
        IdentityManager idm = getIdm();
        IdentityQuery<Role> query = idm.createIdentityQuery(Role.class);
        query.setParameter(Role.PARTITION, realm);
        List<Role> roles = query.getResultList();
        List<RoleModel> roleModels = new ArrayList<RoleModel>();
        for (Role role : roles) {
            roleModels.add(new RoleAdapter(role, idm));
        }
        return roleModels;
    }


    /**
     * Key name, value resource
     *
     * @return
     */
    @Override
    public Map<String, ResourceModel> getResourceMap() {
        Map<String, ResourceModel> resourceMap = new HashMap<String, ResourceModel>();
        for (ResourceModel resource : getResources()) {
            resourceMap.put(resource.getName(), resource);
        }
        return resourceMap;
    }

    @Override
    public List<ResourceModel> getResources() {
        IdentityManager idm = getIdm();
        RelationshipQuery<ResourceRelationship> query = idm.createRelationshipQuery(ResourceRelationship.class);
        query.setParameter(ResourceRelationship.REALM_AGENT, realmAgent);
        List<ResourceRelationship> results = query.getResultList();
        List<ResourceModel> resources = new ArrayList<ResourceModel>();
        for (ResourceRelationship relationship : results) {
            Tier resourceTier = identitySession.findTier(relationship.getResourceId());
            ResourceModel model = new ResourceAdapter(resourceTier,relationship, this, identitySession);
            resources.add(model);
        }

        return resources;
    }

    @Override
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
        ResourceModel resource = new ResourceAdapter(newTier, relationship, this, identitySession);
        resource.addRole("*");
        resource.addScope(new UserAdapter(resourceUser, idm), "*");
        return resource;
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        return getIdm().hasRole(((UserAdapter)user).getUser(), ((RoleAdapter)role).getRole());
    }

    @Override
    public void grantRole(UserModel user, RoleModel role) {
        getIdm().grantRole(((UserAdapter)user).getUser(), ((RoleAdapter)role).getRole());
    }

    @Override
    public Set<String> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getIdm().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        List<Grant> grants = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(realm.getId())) set.add(grant.getRole().getName());
        }
        return set;
    }

    @Override
    public void addScope(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = idm.getRole(roleName);
        if (role == null) throw new RuntimeException("role not found");
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(role);
        idm.add(scope);

    }


    @Override
    public Set<String> getScope(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getIdm().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        HashSet<String> set = new HashSet<String>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(realm.getId())) set.add(rel.getScope().getName());
        }
        return set;
    }

    @Override
    public boolean isRealmAdmin(UserModel agent) {
        RealmAdapter realmModel = (RealmAdapter)new RealmManager(new PicketlinkKeycloakSession(identitySession)).defaultRealm();
        IdentityManager idm = realmModel.getIdm();
        RelationshipQuery<RealmAdminRelationship> query = idm.createRelationshipQuery(RealmAdminRelationship.class);
        query.setParameter(RealmAdminRelationship.REALM, realm.getId());
        query.setParameter(RealmAdminRelationship.ADMIN, ((UserAdapter)agent).getUser());
        List<RealmAdminRelationship> results = query.getResultList();
        return results.size() > 0;
    }

    @Override
    public void addRealmAdmin(UserModel agent) {
        RealmAdapter realmModel = (RealmAdapter)new RealmManager(new PicketlinkKeycloakSession(identitySession)).defaultRealm();
        RealmAdminRelationship relationship = new RealmAdminRelationship();
        relationship.setAdmin(((UserAdapter)agent).getUser());
        relationship.setRealm(realm.getId());
        idm.add(relationship);
    }
}
