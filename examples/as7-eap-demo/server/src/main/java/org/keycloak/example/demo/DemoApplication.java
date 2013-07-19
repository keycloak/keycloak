package org.keycloak.example.demo;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.relationships.RealmAdminRelationship;
import org.keycloak.services.models.relationships.RequiredCredentialRelationship;
import org.keycloak.services.models.relationships.ResourceRelationship;
import org.keycloak.services.models.relationships.ScopeRelationship;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.RegistrationService;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.IdentitySessionFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultIdentitySessionFactory;
import org.picketlink.idm.jpa.internal.ResourceLocalJpaIdentitySessionHandler;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleRole;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DemoApplication extends KeycloakApplication {

    public DemoApplication() {
        super();
        IdentitySession session = factory.createIdentitySession();
        session.getTransaction().begin();
        RealmManager realmManager = new RealmManager(session);
        if (realmManager.defaultRealm() == null) {
            install(realmManager);
        }
        session.getTransaction().commit();
    }

    public void install(RealmManager manager) {
        RealmModel defaultRealm = manager.createRealm(Realm.DEFAULT_REALM, Realm.DEFAULT_REALM);
        defaultRealm.setName(Realm.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.updateRealm();
        defaultRealm.addRequiredCredential(RequiredCredentialModel.PASSWORD);
        defaultRealm.getIdm().add(new SimpleRole(RegistrationService.REALM_CREATOR_ROLE));

        RealmRepresentation rep = loadJson("META-INF/testrealm.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

    }

    public static RealmRepresentation loadJson(String path)
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        try {
            while ( (c = is.read()) != -1)
            {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            //System.out.println(new String(bytes));

            return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
