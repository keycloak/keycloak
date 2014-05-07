package org.keycloak.exportimport;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.model.test.AbstractModelTest;
import org.keycloak.model.test.ImportTest;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.ProviderLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class ExportImportTestBase {

    protected KeycloakSessionFactory factory;

    protected KeycloakSession identitySession;
    protected RealmManager realmManager;

    @Test
    public void testExportImport() throws Exception {
        // Init JPA model
        Config.setModelProvider(getExportModelProvider());
        factory = KeycloakApplication.createSessionFactory();

        // Bootstrap admin realm
        beginTransaction();
        new ApplianceBootstrap().bootstrap(identitySession, "/auth");
        commitTransaction();

        // Classic import of realm to JPA model
        beginTransaction();
        RealmRepresentation rep = AbstractModelTest.loadJson("testrealm.json");
        realmManager = new RealmManager(identitySession);
        RealmModel realm = realmManager.createRealm("demo", rep.getRealm());
        realmManager.importRealm(rep, realm);

        commitTransaction();

        // Full export of realm
        exportModel(factory);

        beginTransaction();
        realm = identitySession.getRealm("demo");
        String wburkeId = realm.getUser("wburke").getId();
        String appId = realm.getApplicationByName("Application").getId();

        // Commit transaction and close JPA now
        commitTransaction();
        factory.close();

        // Bootstrap mongo session and factory
        Config.setModelProvider(getImportModelProvider());
        factory = KeycloakApplication.createSessionFactory();

        // Full import of previous export into mongo
        importModel(factory);

        // Verify it's imported in mongo (reusing ImportTest)
        beginTransaction();
        RealmModel importedRealm = identitySession.getRealm("demo");
        System.out.println("Exported realm: " + realm + ", Imported realm: " + importedRealm);

        Assert.assertEquals(wburkeId, importedRealm.getUser("wburke").getId());
        Assert.assertEquals(appId, importedRealm.getApplicationByName("Application").getId());
        ImportTest.assertDataImportedInRealm(importedRealm);

        // Commit and close Mongo
        commitTransaction();
        factory.close();
    }

    protected abstract String getExportModelProvider();

    protected abstract String getImportModelProvider();

    protected abstract void exportModel(KeycloakSessionFactory factory);

    protected abstract void importModel(KeycloakSessionFactory factory);

    protected void beginTransaction() {
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
    }

    protected void commitTransaction() {
        identitySession.getTransaction().commit();
        identitySession.close();
    }

    protected ExportImportProvider getExportImportProvider() {
        Iterator<ExportImportProvider> providers = ProviderLoader.load(ExportImportProvider.class).iterator();

        if (providers.hasNext()) {
            return providers.next();
        } else {
            throw new IllegalStateException("ExportImportProvider not found");
        }
    }
}
