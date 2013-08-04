package org.keycloak.services.models.picketlink;

import org.bouncycastle.openssl.PEMWriter;
import org.jboss.resteasy.security.PemUtils;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.picketlink.mappings.RealmData;
import org.keycloak.services.models.picketlink.mappings.ResourceData;
import org.keycloak.services.models.picketlink.relationships.OAuthClientRequiredCredentialRelationship;
import org.keycloak.services.models.picketlink.relationships.RealmAdminRelationship;
import org.keycloak.services.models.picketlink.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.picketlink.relationships.ResourceRelationship;
import org.keycloak.services.models.picketlink.relationships.ResourceRequiredCredentialRelationship;
import org.keycloak.services.models.picketlink.relationships.ScopeRelationship;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.TOTPCredential;
import org.picketlink.idm.credential.TOTPCredentials;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.credential.X509CertificateCredentials;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.sample.Grant;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.SampleModel;
import org.picketlink.idm.model.sample.User;
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

    protected RealmData realm;
    protected volatile transient PublicKey publicKey;
    protected volatile transient PrivateKey privateKey;
    protected IdentityManager idm;
    protected PartitionManager partitionManager;
    protected RelationshipManager relationshipManager;
    protected KeycloakSession session;

    public RealmAdapter(KeycloakSession session, RealmData realm, PartitionManager partitionManager) {
        this.session = session;
        this.realm = realm;
        this.partitionManager = partitionManager;
    }

    protected IdentityManager getIdm() {
        if (idm == null) idm = partitionManager.createIdentityManager(realm);
        return idm;
    }

    protected RelationshipManager getRelationshipManager() {
        if (relationshipManager == null) relationshipManager = partitionManager.createRelationshipManager();
        return relationshipManager;
    }

    protected void updateRealm() {
        partitionManager.update(realm);
    }

    @Override
    public String getId() {
        // for some reason picketlink queries by name when finding partition, don't know what ID is used for now
        return realm.getName();
    }

    @Override
    public String getName() {
        return realm.getRealmName();
    }

    @Override
    public void setName(String name) {
        realm.setRealmName(name);
        updateRealm();
    }

    @Override
    public boolean isEnabled() {
        return realm.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        realm.setEnabled(enabled);
        updateRealm();
    }

    @Override
    public boolean isSslNotRequired() {
        return realm.isSslNotRequired();
    }

    @Override
    public void setSslNotRequired(boolean sslNotRequired) {
        realm.setSslNotRequired(sslNotRequired);
        updateRealm();
    }

    @Override
    public boolean isCookieLoginAllowed() {
        return realm.isCookieLoginAllowed();
    }

    @Override
    public void setCookieLoginAllowed(boolean cookieLoginAllowed) {
        realm.setCookieLoginAllowed(cookieLoginAllowed);
        updateRealm();
    }

    @Override
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        updateRealm();
    }

    @Override
    public int getTokenLifespan() {
        return realm.getTokenLifespan();
    }

    @Override
    public void setTokenLifespan(int tokenLifespan) {
        realm.setTokenLifespan(tokenLifespan);
        updateRealm();
    }

    @Override
    public int getAccessCodeLifespan() {
        return realm.getAccessCodeLifespan();
    }

    @Override
    public void setAccessCodeLifespan(int accessCodeLifespan) {
        realm.setAccessCodeLifespan(accessCodeLifespan);
        updateRealm();
    }

    @Override
    public String getPublicKeyPem() {
        return realm.getPublicKeyPem();
    }

    @Override
    public void setPublicKeyPem(String publicKeyPem) {
        realm.setPublicKeyPem(publicKeyPem);
        this.publicKey = null;
        updateRealm();
    }

    @Override
    public String getPrivateKeyPem() {
        return realm.getPrivateKeyPem();
    }

    @Override
    public void setPrivateKeyPem(String privateKeyPem) {
        realm.setPrivateKeyPem(privateKeyPem);
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
        RelationshipQuery<RequiredCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(RequiredCredentialRelationship.class);
        query.setParameter(RequiredCredentialRelationship.REALM, realm.getName());
        List<RequiredCredentialRelationship> results = query.getResultList();
        return getRequiredCredentialModels(results);
    }


    @Override
    public void addResourceRequiredCredential(RequiredCredentialModel cred) {
        ResourceRequiredCredentialRelationship relationship = new ResourceRequiredCredentialRelationship();
        addRequiredCredential(cred, relationship);
    }

    @Override
    public List<RequiredCredentialModel> getResourceRequiredCredentials() {
        RelationshipQuery<ResourceRequiredCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(ResourceRequiredCredentialRelationship.class);
        query.setParameter(ResourceRequiredCredentialRelationship.REALM, realm.getName());
        List<ResourceRequiredCredentialRelationship> results = query.getResultList();
        return getRequiredCredentialModels(results);
    }

    @Override
    public void addOAuthClientRequiredCredential(RequiredCredentialModel cred) {
        OAuthClientRequiredCredentialRelationship relationship = new OAuthClientRequiredCredentialRelationship();
        addRequiredCredential(cred, relationship);
    }

    @Override
    public List<RequiredCredentialModel> getOAuthClientRequiredCredentials() {
        RelationshipQuery<OAuthClientRequiredCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRequiredCredentialRelationship.class);
        query.setParameter(ResourceRequiredCredentialRelationship.REALM, realm.getName());
        List<OAuthClientRequiredCredentialRelationship> results = query.getResultList();
        return getRequiredCredentialModels(results);
    }



    @Override
    public void addRequiredCredential(RequiredCredentialModel cred) {
        RequiredCredentialRelationship relationship = new RequiredCredentialRelationship();
        addRequiredCredential(cred, relationship);
    }


    protected List<RequiredCredentialModel> getRequiredCredentialModels(List<? extends RequiredCredentialRelationship> results) {
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
    protected void addRequiredCredential(RequiredCredentialModel cred, RequiredCredentialRelationship relationship) {
        relationship.setCredentialType(cred.getType());
        relationship.setInput(cred.isInput());
        relationship.setSecret(cred.isSecret());
        relationship.setRealm(realm.getName());
        getRelationshipManager().add(relationship);
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
        User user = findPicketlinkUser(name);
        if (user == null) return null;
        return new UserAdapter(user, getIdm());
    }

    protected User findPicketlinkUser(String name) {
        return SampleModel.getUser(getIdm(), name);
    }

    @Override
    public UserAdapter addUser(String username) {
        User user = findPicketlinkUser(username);
        if (user != null) throw new IllegalStateException("User already exists");
        user = new User(username);
        getIdm().add(user);
        return new UserAdapter(user, getIdm());
    }

    @Override
    public RoleAdapter getRole(String name) {
        Role role = SampleModel.getRole(getIdm(), name);
        if (role == null) return null;
        return new RoleAdapter(role, getIdm());
    }

    @Override
    public RoleModel getRoleById(String id) {
        IdentityQuery<Role> query = getIdm().createIdentityQuery(Role.class);
        query.setParameter(IdentityType.ID, id);
        List<Role> roles = query.getResultList();
        if (roles.size() == 0) return null;
        return new RoleAdapter(roles.get(0), getIdm());
    }

    @Override
    public RoleAdapter addRole(String name) {
        Role role = new Role(name);
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
        RelationshipQuery<ResourceRelationship> query = getRelationshipManager().createRelationshipQuery(ResourceRelationship.class);
        query.setParameter(ResourceRelationship.REALM, realm.getName());
        List<ResourceRelationship> results = query.getResultList();
        List<ResourceModel> resources = new ArrayList<ResourceModel>();
        for (ResourceRelationship relationship : results) {
            ResourceData resource = partitionManager.getPartition(ResourceData.class, relationship.getResource());
            ResourceModel model = new ResourceAdapter(resource, this, partitionManager);
            resources.add(model);
        }

        return resources;
    }

    @Override
    public ResourceModel addResource(String name) {
        ResourceData resourceData = new ResourceData(RealmManager.generateId());
        User resourceUser = new User(name);
        idm.add(resourceUser);
        resourceData.setResourceUser(resourceUser);
        resourceData.setResourceName(name);
        resourceData.setResourceUser(resourceUser);
        partitionManager.add(resourceData);
        ResourceRelationship resourceRelationship = new ResourceRelationship();
        resourceRelationship.setRealm(realm.getName());
        resourceRelationship.setResource(resourceData.getName());
        getRelationshipManager().add(resourceRelationship);
        ResourceModel resource = new ResourceAdapter(resourceData, this, partitionManager);
        resource.addRole("*");
        resource.addScope(new UserAdapter(resourceUser, idm), "*");
        return resource;
    }

    @Override
    public boolean hasRole(UserModel user, RoleModel role) {
        return SampleModel.hasRole(getRelationshipManager(), ((UserAdapter) user).getUser(), ((RoleAdapter) role).getRole());
    }

    @Override
    public boolean hasRole(UserModel user, String role) {
        RoleModel roleModel = getRole(role);
        return hasRole(user, roleModel);
    }


    @Override
    public void grantRole(UserModel user, RoleModel role) {
        SampleModel.grantRole(getRelationshipManager(), ((UserAdapter) user).getUser(), ((RoleAdapter) role).getRole());
    }

    @Override
    public Set<String> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
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
        Role role = SampleModel.getRole(idm, roleName);
        if (role == null) throw new RuntimeException("role not found");
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(role);
        getRelationshipManager().add(scope);
    }


    @Override
    public Set<String> getScope(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
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
        RealmAdapter realmModel = (RealmAdapter)new RealmManager(session).defaultRealm();
        RelationshipQuery<RealmAdminRelationship> query = getRelationshipManager().createRelationshipQuery(RealmAdminRelationship.class);
        query.setParameter(RealmAdminRelationship.REALM, realm.getName());
        query.setParameter(RealmAdminRelationship.ADMIN, ((UserAdapter)agent).getUser());
        List<RealmAdminRelationship> results = query.getResultList();
        return results.size() > 0;
    }

    @Override
    public void addRealmAdmin(UserModel agent) {
        RealmAdminRelationship relationship = new RealmAdminRelationship();
        relationship.setAdmin(((UserAdapter)agent).getUser());
        relationship.setRealm(realm.getName());
        getRelationshipManager().add(relationship);
    }
}
