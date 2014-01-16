package org.keycloak.models.picketlink;

import org.bouncycastle.openssl.PEMWriter;
import org.keycloak.util.PemUtils;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.IdGenerator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.picketlink.mappings.ApplicationData;
import org.keycloak.models.picketlink.mappings.RealmData;
import org.keycloak.models.picketlink.relationships.ApplicationRelationship;
import org.keycloak.models.picketlink.relationships.OAuthClientRelationship;
import org.keycloak.models.picketlink.relationships.OAuthClientRequiredCredentialRelationship;
import org.keycloak.models.picketlink.relationships.RequiredApplicationCredentialRelationship;
import org.keycloak.models.picketlink.relationships.RequiredCredentialRelationship;
import org.keycloak.models.picketlink.relationships.ScopeRelationship;
import org.keycloak.models.picketlink.relationships.SocialLinkRelationship;
import org.picketlink.idm.IdentityManagementException;
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
import org.picketlink.idm.query.QueryParameter;
import org.picketlink.idm.query.RelationshipQuery;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;

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
    private PasswordPolicy passwordPolicy;

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
    public boolean isSocial() {
        return realm.isSocial();
    }

    @Override
    public void setSocial(boolean social) {
        realm.setSocial(social);
        updateRealm();
    }

    @Override
    public boolean isUpdateProfileOnInitialSocialLogin() {
        return realm.isUpdateProfileOnInitialSocialLogin();
    }

    @Override
    public void setUpdateProfileOnInitialSocialLogin(boolean updateProfileOnInitialSocialLogin) {
        realm.setUpdateProfileOnInitialSocialLogin(updateProfileOnInitialSocialLogin);
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
    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        realm.setRegistrationAllowed(registrationAllowed);
        updateRealm();
    }

    @Override
    public boolean isVerifyEmail() {
        return realm.isVerifyEmail();
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        realm.setVerifyEmail(verifyEmail);
        updateRealm();
    }

    @Override
    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPassword) {
        realm.setResetPasswordAllowed(resetPassword);
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
    public int getAccessCodeLifespanUserAction() {
        return realm.getAccessCodeLifespanUserAction();
    }

    @Override
    public void setAccessCodeLifespanUserAction(int accessCodeLifespanUserAction) {
        realm.setAccessCodeLifespanUserAction(accessCodeLifespanUserAction);
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
        List<RequiredCredentialRelationship> results = getRequiredCredentialRelationships();
        return getRequiredCredentialModels(results);
    }

    protected List<RequiredCredentialRelationship> getRequiredCredentialRelationships() {
        RelationshipQuery<RequiredCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(RequiredCredentialRelationship.class);
        query.setParameter(RequiredCredentialRelationship.REALM, realm.getName());
        return query.getResultList();
    }


    public void addRequiredApplicationCredential(RequiredCredentialModel cred) {
        RequiredApplicationCredentialRelationship relationship = new RequiredApplicationCredentialRelationship();
        addRequiredCredential(cred, relationship);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredApplicationCredentials() {
        List<RequiredApplicationCredentialRelationship> results = getResourceRequiredCredentialRelationships();
        return getRequiredCredentialModels(results);
    }

    protected List<RequiredApplicationCredentialRelationship> getResourceRequiredCredentialRelationships() {
        RelationshipQuery<RequiredApplicationCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(RequiredApplicationCredentialRelationship.class);
        query.setParameter(RequiredApplicationCredentialRelationship.REALM, realm.getName());
        return query.getResultList();
    }

    public void addRequiredOAuthClientCredential(RequiredCredentialModel cred) {
        OAuthClientRequiredCredentialRelationship relationship = new OAuthClientRequiredCredentialRelationship();
        addRequiredCredential(cred, relationship);
    }

    @Override
    public List<RequiredCredentialModel> getRequiredOAuthClientCredentials() {
        List<OAuthClientRequiredCredentialRelationship> results = getOAuthClientRequiredCredentialRelationships();
        return getRequiredCredentialModels(results);
    }

    protected List<OAuthClientRequiredCredentialRelationship> getOAuthClientRequiredCredentialRelationships() {
        RelationshipQuery<OAuthClientRequiredCredentialRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRequiredCredentialRelationship.class);
        query.setParameter(RequiredApplicationCredentialRelationship.REALM, realm.getName());
        return query.getResultList();
    }

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
            model.setFormLabel(relationship.getFormLabel());
            rtn.add(model);
        }
        return rtn;
    }
    protected void addRequiredCredential(RequiredCredentialModel cred, RequiredCredentialRelationship relationship) {
        relationship.setCredentialType(cred.getType());
        relationship.setInput(cred.isInput());
        relationship.setSecret(cred.isSecret());
        relationship.setRealm(realm.getName());
        relationship.setFormLabel(cred.getFormLabel());
        getRelationshipManager().add(relationship);
    }

    @Override
    public void updateRequiredCredentials(Set<String> creds) {
        List<RequiredCredentialRelationship> relationships = getRequiredCredentialRelationships();
        RelationshipManager rm = getRelationshipManager();
        Set<String> already = new HashSet<String>();
        for (RequiredCredentialRelationship rel : relationships) {
            if (!creds.contains(rel.getCredentialType())) {
                rm.remove(rel);
            } else {
                already.add(rel.getCredentialType());
            }
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredCredential(cred);
            }
        }
    }

    @Override
    public void updateRequiredOAuthClientCredentials(Set<String> creds) {
        List<OAuthClientRequiredCredentialRelationship> relationships = getOAuthClientRequiredCredentialRelationships();
        RelationshipManager rm = getRelationshipManager();
        Set<String> already = new HashSet<String>();
        for (RequiredCredentialRelationship rel : relationships) {
            if (!creds.contains(rel.getCredentialType())) {
                rm.remove(rel);
            } else {
                already.add(rel.getCredentialType());
            }
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredOAuthClientCredential(cred);
            }
        }
    }

    @Override
    public void updateRequiredApplicationCredentials(Set<String> creds) {
        List<RequiredApplicationCredentialRelationship> relationships = getResourceRequiredCredentialRelationships();
        RelationshipManager rm = getRelationshipManager();
        Set<String> already = new HashSet<String>();
        for (RequiredCredentialRelationship rel : relationships) {
            if (!creds.contains(rel.getCredentialType())) {
                rm.remove(rel);
            } else {
                already.add(rel.getCredentialType());
            }
        }
        for (String cred : creds) {
            if (!already.contains(cred)) {
                addRequiredResourceCredential(cred);
            }
        }
    }


    @Override
    public void addRequiredCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredCredential(model);
    }

    @Override
    public void addRequiredOAuthClientCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredOAuthClientCredential(model);
    }

    @Override
    public void addRequiredResourceCredential(String type) {
        RequiredCredentialModel model = initRequiredCredentialModel(type);
        addRequiredApplicationCredential(model);
    }

    protected RequiredCredentialModel initRequiredCredentialModel(String type) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(type);
        if (model == null) {
            throw new RuntimeException("Unknown credential type " + type);
        }
        return model;
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
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            Password password = new Password(cred.getValue());
            idm.updateCredential(((UserAdapter)user).getUser(), password);
        } else if (cred.getType().equals(UserCredentialModel.TOTP)) {
            TOTPCredential totp = new TOTPCredential(cred.getValue());
            totp.setDevice(cred.getDevice());
            idm.updateCredential(((UserAdapter)user).getUser(), totp);
        } else if (cred.getType().equals(UserCredentialModel.CLIENT_CERT)) {
            X509Certificate cert = null;
            try {
                cert = PemUtils.decodeCertificate(cred.getValue());
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

    @Override
    public UserModel getUserByEmail(String email) {
        IdentityQuery<User> query = getIdm().createIdentityQuery(User.class);
        query.setParameter(User.EMAIL, email);
        List<User> users = query.getResultList();
        return users.isEmpty() ? null : new UserAdapter(users.get(0), getIdm());
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
    public boolean removeUser(String name) {
        User user = findPicketlinkUser(name);
        if (user == null) {
            return false;
        }
        getIdm().remove(user);
        return true;
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
    public boolean removeRoleById(String id) {
        try {
            getIdm().remove(getIdm().lookupIdentityById(Role.class, id));
            return true;
        } catch (IdentityManagementException e) {
            return false;
        }
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
    public Map<String, ApplicationModel> getApplicationNameMap() {
        Map<String, ApplicationModel> resourceMap = new HashMap<String, ApplicationModel>();
        for (ApplicationModel resource : getApplications()) {
            resourceMap.put(resource.getName(), resource);
        }
        return resourceMap;
    }

    /**
     * Makes sure that the resource returned is owned by the realm
     *
     * @return
     */
    @Override
    public ApplicationModel getApplicationById(String id) {
        RelationshipQuery<ApplicationRelationship> query = getRelationshipManager().createRelationshipQuery(ApplicationRelationship.class);
        query.setParameter(ApplicationRelationship.REALM, realm.getName());
        query.setParameter(ApplicationRelationship.APPLICATION, id);
        List<ApplicationRelationship> results = query.getResultList();
        if (results.size() == 0) return null;
        ApplicationData resource = partitionManager.getPartition(ApplicationData.class, id);
        ApplicationModel model = new ApplicationAdapter(resource, this, partitionManager);
        return model;
    }

    @Override
    public ApplicationModel getApplicationByName(String name) {
        return getApplicationNameMap().get(name);
    }



    @Override
    public List<ApplicationModel> getApplications() {
        RelationshipQuery<ApplicationRelationship> query = getRelationshipManager().createRelationshipQuery(ApplicationRelationship.class);
        query.setParameter(ApplicationRelationship.REALM, realm.getName());
        List<ApplicationRelationship> results = query.getResultList();
        List<ApplicationModel> resources = new ArrayList<ApplicationModel>();
        for (ApplicationRelationship relationship : results) {
            ApplicationData resource = partitionManager.getPartition(ApplicationData.class, relationship.getApplication());
            ApplicationModel model = new ApplicationAdapter(resource, this, partitionManager);
            resources.add(model);
        }

        return resources;
    }

    @Override
    public ApplicationModel addApplication(String name) {
        ApplicationData applicationData = new ApplicationData(IdGenerator.generateId());
        User resourceUser = new User(name);
        idm.add(resourceUser);
        applicationData.setResourceUser(resourceUser);
        applicationData.setResourceName(name);
        partitionManager.add(applicationData);
        ApplicationRelationship resourceRelationship = new ApplicationRelationship();
        resourceRelationship.setRealm(realm.getName());
        resourceRelationship.setApplication(applicationData.getName());
        getRelationshipManager().add(resourceRelationship);
        ApplicationModel resource = new ApplicationAdapter(applicationData, this, partitionManager);
        return resource;
    }

    @Override
    public boolean removeApplication(String id) {
        RelationshipQuery<ApplicationRelationship> query = getRelationshipManager().createRelationshipQuery(ApplicationRelationship.class);
        query.setParameter(ApplicationRelationship.REALM, realm.getName());
        query.setParameter(ApplicationRelationship.APPLICATION, id);
        List<ApplicationRelationship> results = query.getResultList();
        if (results.size() == 0) return false;
        ApplicationRelationship relationship = results.get(0);
        ApplicationData application = partitionManager.getPartition(ApplicationData.class, relationship.getApplication());
        partitionManager.remove(application);
        return true;
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
    public void deleteRoleMapping(UserModel user, RoleModel role) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        query.setParameter(Grant.ROLE, ((RoleAdapter)role).getRole());
        List<Grant> grants = query.getResultList();
        for (Grant grant : grants) {
            getRelationshipManager().remove(grant);
        }
    }

    @Override
    public Set<String> getRoleMappingValues(UserModel user) {
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
    public List<RoleModel> getRoleMappings(UserModel user) {
        RelationshipQuery<Grant> query = getRelationshipManager().createRelationshipQuery(Grant.class);
        query.setParameter(Grant.ASSIGNEE, ((UserAdapter)user).getUser());
        List<Grant> grants = query.getResultList();
        List<RoleModel> set = new ArrayList<RoleModel>();
        for (Grant grant : grants) {
            if (grant.getRole().getPartition().getId().equals(realm.getId())) set.add(new RoleAdapter(grant.getRole(), getIdm()));
        }
        return set;
    }

    @Override
    public void addScopeMapping(UserModel agent, String roleName) {
        IdentityManager idm = getIdm();
        Role role = SampleModel.getRole(idm, roleName);
        if (role == null) throw new RuntimeException("role not found");
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter)agent).getUser());
        scope.setScope(role);
        getRelationshipManager().add(scope);
    }

    @Override
    public void addScopeMapping(UserModel agent, RoleModel role) {
        ScopeRelationship scope = new ScopeRelationship();
        scope.setClient(((UserAdapter) agent).getUser());
        scope.setScope(((RoleAdapter)role).getRole());
        getRelationshipManager().add(scope);
    }

    @Override
    public void deleteScopeMapping(UserModel user, RoleModel role) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)user).getUser());
        query.setParameter(ScopeRelationship.SCOPE, ((RoleAdapter)role).getRole());
        List<ScopeRelationship> grants = query.getResultList();
        for (ScopeRelationship grant : grants) {
            getRelationshipManager().remove(grant);
        }
    }

    @Override
    public OAuthClientModel addOAuthClient(String name) {
        User client = new User(name);
        getIdm().add(client);
        OAuthClientRelationship rel = new OAuthClientRelationship();
        rel.setOauthAgent(client);
        rel.setRealm(realm.getName());
        getRelationshipManager().add(rel);
        return new OAuthClientAdapter(rel, getIdm(), getRelationshipManager());
    }

    @Override
    public boolean removeOAuthClient(String id) {
        RelationshipQuery<OAuthClientRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRelationship.class);
        query.setParameter(OAuthClientRelationship.REALM, realm.getName());
        query.setParameter(OAuthClientRelationship.ID, id);
        List<OAuthClientRelationship> results = query.getResultList();
        if (results.size() == 0) return false;
        OAuthClientRelationship relationship = results.get(0);
        getRelationshipManager().remove(relationship);
        return true;
    }


    @Override
    public OAuthClientModel getOAuthClient(String name) {
        User user = findPicketlinkUser(name);
        if (user == null) return null;
        RelationshipQuery<OAuthClientRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRelationship.class);
        query.setParameter(OAuthClientRelationship.OAUTH_AGENT, user);
        List<OAuthClientRelationship> results = query.getResultList();
        if (results.size() == 0) return null;
        return new OAuthClientAdapter(results.get(0), getIdm(), getRelationshipManager());
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id) {
        RelationshipQuery<OAuthClientRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRelationship.class);
        query.setParameter(OAuthClientRelationship.REALM, realm.getName());
        query.setParameter(OAuthClientRelationship.ID, id);
        List<OAuthClientRelationship> results = query.getResultList();
        if (results.size() == 0) return null;
        return new OAuthClientAdapter(results.get(0), getIdm(), getRelationshipManager());
    }


    @Override
    public List<OAuthClientModel> getOAuthClients() {
        RelationshipQuery<OAuthClientRelationship> query = getRelationshipManager().createRelationshipQuery(OAuthClientRelationship.class);
        query.setParameter(OAuthClientRelationship.REALM, realm.getName());
        List<OAuthClientRelationship> results = query.getResultList();
        List<OAuthClientModel> list = new ArrayList<OAuthClientModel>();
        for (OAuthClientRelationship rel : results) {
            list.add(new OAuthClientAdapter(rel, getIdm(), getRelationshipManager()));
        }
        return list;
    }

    @Override
    public List<RoleModel> getScopeMappings(UserModel agent) {
        RelationshipQuery<ScopeRelationship> query = getRelationshipManager().createRelationshipQuery(ScopeRelationship.class);
        query.setParameter(ScopeRelationship.CLIENT, ((UserAdapter)agent).getUser());
        List<ScopeRelationship> scope = query.getResultList();
        List<RoleModel> roles = new ArrayList<RoleModel>();
        for (ScopeRelationship rel : scope) {
            if (rel.getScope().getPartition().getId().equals(realm.getId())) roles.add(new RoleAdapter(rel.getScope(), getIdm()));
        }
        return roles;
    }

    @Override
    public Set<String> getScopeMappingValues(UserModel agent) {
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
    public List<String> getDefaultRoles() {
        if (realm.getDefaultRoles() == null) return Collections.emptyList();
        List<String> list = new ArrayList<String>();
        for (String role : realm.getDefaultRoles()) {
            RoleModel model = getRole(role);
            if (model == null) throw new RuntimeException("default role missing");
            list.add(role);
        }
        return list;
    }

    @Override
    public void addDefaultRole(String name) {
        if (getRole(name) == null) {
            addRole(name);
        }

        String[] defaultRoles = realm.getDefaultRoles();
        if (defaultRoles == null) {
            defaultRoles = new String[1];
        } else {
            defaultRoles = Arrays.copyOf(defaultRoles, defaultRoles.length + 1);
        }
        defaultRoles[defaultRoles.length - 1] = name;

        realm.setDefaultRoles(defaultRoles);
        updateRealm();
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        for (String name : defaultRoles) {
            if (getRole(name) == null) {
                addRole(name);
            }
        }

        realm.setDefaultRoles(defaultRoles);
        updateRealm();
    }

    @Override
    public UserModel getUserBySocialLink(SocialLinkModel socialLink) {
        RelationshipQuery<SocialLinkRelationship> query = getRelationshipManager().createRelationshipQuery(SocialLinkRelationship.class);
        query.setParameter(SocialLinkRelationship.SOCIAL_PROVIDER, socialLink.getSocialProvider());
        query.setParameter(SocialLinkRelationship.SOCIAL_USERNAME, socialLink.getSocialUsername());
        query.setParameter(SocialLinkRelationship.REALM, realm.getName());
        List<SocialLinkRelationship> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for socialProvider=" + socialLink.getSocialProvider() +
                    ", socialUsername=" + socialLink.getSocialUsername() + ", results=" + results);
        } else {
            User user = results.get(0).getUser();
            return new UserAdapter(user, getIdm());
        }
    }

    @Override
    public Set<SocialLinkModel> getSocialLinks(UserModel user) {
        RelationshipQuery<SocialLinkRelationship> query = getRelationshipManager().createRelationshipQuery(SocialLinkRelationship.class);
        query.setParameter(SocialLinkRelationship.USER, ((UserAdapter)user).getUser());
        List<SocialLinkRelationship> plSocialLinks = query.getResultList();

        Set<SocialLinkModel> results = new HashSet<SocialLinkModel>();
        for (SocialLinkRelationship relationship : plSocialLinks) {
            results.add(new SocialLinkModel(relationship.getSocialProvider(), relationship.getSocialUsername()));
        }
        return results;
    }

    @Override
    public void addSocialLink(UserModel user, SocialLinkModel socialLink) {
        SocialLinkRelationship relationship = new SocialLinkRelationship();
        relationship.setUser(((UserAdapter)user).getUser());
        relationship.setSocialProvider(socialLink.getSocialProvider());
        relationship.setSocialUsername(socialLink.getSocialUsername());
        relationship.setRealm(realm.getName());

        getRelationshipManager().add(relationship);
    }

    @Override
    public void removeSocialLink(UserModel user, SocialLinkModel socialLink) {
        SocialLinkRelationship relationship = new SocialLinkRelationship();
        relationship.setUser(((UserAdapter)user).getUser());
        relationship.setSocialProvider(socialLink.getSocialProvider());
        relationship.setSocialUsername(socialLink.getSocialUsername());
        relationship.setRealm(realm.getName());

        getRelationshipManager().remove(relationship);
    }

    @Override
    public List<UserModel> getUsers() {
        List<UserModel> userModels = new ArrayList<UserModel>();
        IdentityQuery<User> query = getIdm().createIdentityQuery(User.class);
        for (User u : query.getResultList()) {
            userModels.add(new UserAdapter(u, idm));
        }
        return userModels;
    }

    @Override
    public List<UserModel> searchForUser(String search) {
        QueryParameter[] params = new QueryParameter[] { User.LOGIN_NAME, User.FIRST_NAME, User.LAST_NAME, User.EMAIL };

        List<UserModel> userModels = new ArrayList<UserModel>();
        for (QueryParameter p : params) {
            IdentityQuery<User> query = getIdm().createIdentityQuery(User.class);
            query.setParameter(p, search.toLowerCase());
            for (User u : query.getResultList()) {
                userModels.add(new UserAdapter(u, idm));
            }
        }
        return userModels;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes) {
        IdentityQuery<User> query = getIdm().createIdentityQuery(User.class);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equals(UserModel.LOGIN_NAME)) {
                query.setParameter(User.LOGIN_NAME, entry.getValue());
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                query.setParameter(User.FIRST_NAME, entry.getValue());

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                query.setParameter(User.LAST_NAME, entry.getValue());

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                query.setParameter(User.EMAIL, entry.getValue());
            }
        }
        List<User> users = query.getResultList();
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (User user : users) {
            userModels.add(new UserAdapter(user, idm));
        }
        return userModels;
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        return realm.getSmtpConfig();
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        realm.setSmtpConfig(smtpConfig);
        updateRealm();
    }

    @Override
    public Map<String, String> getSocialConfig() {
        return realm.getSocialConfig();
    }

    @Override
    public void setSocialConfig(Map<String, String> socialConfig) {
        realm.setSocialConfig(socialConfig);
        updateRealm();
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = new PasswordPolicy(realm.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        realm.setPasswordPolicy(policy.toString());
        updateRealm();
    }
}
