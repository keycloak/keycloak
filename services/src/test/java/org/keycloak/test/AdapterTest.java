package org.keycloak.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.model.RealmManager;
import org.keycloak.services.model.RealmModel;
import org.keycloak.services.model.RealmResourceRelationship;
import org.keycloak.services.model.RequiredCredentialModel;
import org.keycloak.services.model.RequiredCredentialRelationship;
import org.keycloak.services.model.ScopeRelationship;
import org.keycloak.services.model.UserCredentialModel;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.file.internal.FileUtils;
import org.picketlink.idm.internal.IdentityManagerFactory;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest
{
   private static IdentityManagerFactory factory;
   public static final String WORKING_DIRECTORY = "/tmp/keycloak";
   public RealmManager adapter;
   public RealmModel realmModel;
   @Before
   public void before() throws Exception
   {
      after();
      factory = createFactory();
      adapter = new RealmManager(factory);
   }

   private static IdentityManagerFactory createFactory() {
      IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

      builder
              .stores()
              .file()
              .addRealm(Realm.DEFAULT_REALM)
              .workingDirectory(WORKING_DIRECTORY)
              .preserveState(true)
              .supportAllFeatures()
              .supportRelationshipType(RealmResourceRelationship.class, RequiredCredentialRelationship.class, ScopeRelationship.class);

      return new IdentityManagerFactory(builder.build());
   }

   @After
   public void after() throws Exception
   {
      File file = new File(WORKING_DIRECTORY);
      FileUtils.delete(file);
      Thread.sleep(10); // my windows machine seems to have delays on deleting files sometimes
   }

   @Test
   public void test1CreateRealm() throws Exception
   {
      realmModel = adapter.create("JUGGLER");
      realmModel.setAccessCodeLifespan(100);
      realmModel.setCookieLoginAllowed(true);
      realmModel.setEnabled(true);
      realmModel.setName("JUGGLER");
      realmModel.setPrivateKeyPem("0234234");
      realmModel.setPublicKeyPem("0234234");
      realmModel.setTokenLifespan(1000);
      realmModel.updateRealm();

      System.out.println(realmModel.getId());
      realmModel = adapter.getRealm(realmModel.getId());
      Assert.assertNotNull(realmModel);
      Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
      Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
      Assert.assertEquals(realmModel.isEnabled(), true);
      Assert.assertEquals(realmModel.getName(), "JUGGLER");
      Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
      Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
   }

   @Test
   public void test2RequiredCredential() throws Exception
   {
      test1CreateRealm();
      RequiredCredentialModel creds = new RequiredCredentialModel();
      creds.setSecret(true);
      creds.setType(RequiredCredentialRepresentation.PASSWORD);
      creds.setInput(true);
      realmModel.addRequiredCredential(creds);
      creds = new RequiredCredentialModel();
      creds.setSecret(true);
      creds.setType(RequiredCredentialRepresentation.TOTP);
      creds.setInput(true);
      realmModel.addRequiredCredential(creds);
      List<RequiredCredentialModel> storedCreds = realmModel.getRequiredCredentials();
      Assert.assertEquals(2, storedCreds.size());
      boolean totp = false;
      boolean password = false;
      for (RequiredCredentialModel cred : storedCreds)
      {
         if (cred.getType().equals(RequiredCredentialRepresentation.PASSWORD)) password = true;
         else if (cred.getType().equals(RequiredCredentialRepresentation.TOTP)) totp = true;
      }
      Assert.assertTrue(totp);
      Assert.assertTrue(password);
   }

   @Test
   public void testCredentialValidation() throws Exception
   {
      test1CreateRealm();
      User user = new SimpleUser("bburke");
      realmModel.getIdm().add(user);
      UserCredentialModel cred = new UserCredentialModel();
      cred.setType(RequiredCredentialRepresentation.PASSWORD);
      cred.setValue("geheim");
      realmModel.updateCredential(user, cred);
      IdentityManager idm = realmModel.getIdm();
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user.getLoginName(), new Password("geheim"));
      idm.validateCredentials(creds);
      Assert.assertEquals(creds.getStatus(), Credentials.Status.VALID);
   }

   @Test
   public void testRoles() throws Exception
   {
      test1CreateRealm();
      IdentityManager idm = realmModel.getIdm();
      idm.add(new SimpleRole("admin"));
      idm.add(new SimpleRole("user"));
      List<Role> roles = realmModel.getRoles();
      Assert.assertEquals(2, roles.size());
      SimpleUser user = new SimpleUser("bburke");
      idm.add(user);
      Role role = idm.getRole("user");
      idm.grantRole(user, role);
      Assert.assertTrue(idm.hasRole(user, role));
   }


}
