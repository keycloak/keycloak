package org.keycloak.model.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.users.Credentials;
import org.keycloak.models.users.Feature;
import org.keycloak.models.users.User;
import org.keycloak.models.users.UserProvider;
import org.keycloak.models.users.UserProviderFactory;
import org.keycloak.models.users.UserSpi;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.provider.ProviderFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractUserProviderTest {

    private ProviderFactory<UserProvider> factory;
    private UserProvider provider;
    private String userId;
    private String userId2;
    private String userId3;

    @Before
    public void before() {
        String providerId = getProviderId();
        ServiceLoader<UserProviderFactory> factories = ServiceLoader.load(UserProviderFactory.class);
        for (UserProviderFactory f : factories) {
            if (f.getId().equals(providerId)) {
                factory = f;
                factory.init(Config.scope(new UserSpi().getName(), providerId));
            }
        }

        provider = factory.create(null);

        userId = "1";
        userId2 = "2";
        userId3 = "1";
    }

    @After
    public void after() {
        provider.getTransaction().begin();
        provider.onRealmRemoved("test-realm");
        provider.getTransaction().commit();
    }

    protected abstract String getProviderId();

    @Test
    public void persistUsers() {
        provider.getTransaction().begin();

        Set<String> roles = new HashSet<String>();
        roles.add("a");
        roles.add("a1");

        User user = provider.addUser(userId, "user", roles, "test-realm");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");
        user.setAttribute("a", "a1");
        user.setAttribute("b", "b1");

        Set<String> roles2 = new HashSet<String>();
        roles2.add("a");
        roles2.add("a2");

        User user2 = provider.addUser(userId2, "user2", roles2, "test-realm");
        user2.setFirstName("first-name2");
        user2.setLastName("last-name2");
        user2.setEmail("email2");
        user2.setAttribute("a", "a2");
        user2.setAttribute("b", "b2");

        User user3 = provider.addUser(userId3, "user", roles2, "test-realm2");
        user3.setFirstName("first-name");
        user3.setLastName("last-name");
        user3.setEmail("email");
        user3.setAttribute("a", "a1");
        user3.setAttribute("b", "b1");

        provider.getTransaction().commit();

        User persisted = provider.getUserById(userId, "test-realm");
        User persisted2 = provider.getUserById(userId2, "test-realm");
        User persisted3 = provider.getUserById(userId3, "test-realm2");

        assertUser(user, persisted);
        assertUser(user2, persisted2);
        assertUser(user3, persisted3);
    }

    @Test
    public void getUserByEmail() {
        persistUsers();

        assertEquals(userId, provider.getUserByEmail("email", "test-realm").getId());
        assertEquals(userId2, provider.getUserByEmail("email2", "test-realm").getId());
    }

    @Test
    public void getUserByUsername() {
        persistUsers();

        assertEquals(userId, provider.getUserByUsername("user", "test-realm").getId());
        assertEquals(userId2, provider.getUserByUsername("user2", "test-realm").getId());
    }

    @Test
    public void readAndUpdateCredentials() {
        if (provider.supports(Feature.READ_CREDENTIALS) && provider.supports(Feature.UPDATE_CREDENTIALS)) {
            persistUsers();

            provider.getTransaction().begin();

            User user = provider.getUserById(userId, "test-realm");
            User user2 = provider.getUserById(userId2, "test-realm");
            User user3 = provider.getUserById(userId3, "test-realm2");

            byte[] salt = Pbkdf2PasswordEncoder.getSalt();
            Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder(salt);

            user.updateCredential(new Credentials(UserCredentialModel.PASSWORD, salt, encoder.encode("password", 1000), 1000, null));
            user.updateCredential(new Credentials(UserCredentialModel.TOTP, "totp-secret", null));

            user2.updateCredential(new Credentials(UserCredentialModel.PASSWORD, salt, encoder.encode("password2", 1000), 1000, null));
            user3.updateCredential(new Credentials(UserCredentialModel.PASSWORD, salt, encoder.encode("password3", 1000), 1000, null));

            provider.getTransaction().commit();

            User persisted = provider.getUserById(userId, "test-realm");
            assertEquals(2, persisted.getCredentials().size());
            assertPassword(persisted, "password");
            assertTotp(user, "totp-secret");

            User persisted2 = provider.getUserById(userId2, "test-realm");
            assertEquals(1, persisted2.getCredentials().size());
            assertPassword(persisted2, "password2");

            User persisted3 = provider.getUserById(userId3, "test-realm2");
            assertEquals(1, persisted3.getCredentials().size());
            assertPassword(persisted3, "password3");
        }
    }

    @Test
    public void userSearch() throws Exception {
        provider.getTransaction().begin();
        {
            User user = provider.addUser("bill-burke", "bill-burke", null, "test-realm");
            user.setLastName("Burke");
            user.setFirstName("Bill");
            user.setEmail("bburke@redhat.com");

            User user2 = provider.addUser("knut-ole", "knut-ole", null, "test-realm");
            user2.setFirstName("Knut Ole");
            user2.setLastName("Alver");
            user2.setEmail("knut@redhat.com");

            User user3 = provider.addUser("ole-alver", "ole-alver", null, "test-realm");
            user3.setFirstName("Ole");
            user3.setLastName("Alver Veland");
            user3.setEmail("knut2@redhat.com");
        }

        provider.getTransaction().commit();

        assertUsers(provider.searchForUser("total junk query", "test-realm"));
        assertUsers(provider.searchForUser("Bill Burke", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("bill burk", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("bill burk", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("ole alver", "test-realm"), "knut-ole", "ole-alver");
        assertUsers(provider.searchForUser("bburke@redhat.com", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("rke@redhat.com", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("bburke", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("BurK", "test-realm"), "bill-burke");
        assertUsers(provider.searchForUser("Burke", "test-realm"), "bill-burke");

        provider.getTransaction().begin();
        {
            User user = provider.addUser("monica-burke", "monica-burke", null, "test-realm");
            user.setLastName("Burke");
            user.setFirstName("Monica");
            user.setEmail("mburke@redhat.com");
        }

        {
            User user = provider.addUser("stian-thorgersen", "stian-thorgersen", null, "test-realm");
            user.setLastName("Thorgersen");
            user.setFirstName("Stian");
            user.setEmail("thor@redhat.com");
        }
        provider.getTransaction().commit();

        assertUsers(provider.searchForUser("Monica Burke", "test-realm"), "monica-burke");
        assertUsers(provider.searchForUser("mburke@redhat.com", "test-realm"), "monica-burke");
        assertUsers(provider.searchForUser("mburke", "test-realm"), "monica-burke");
        assertUsers(provider.searchForUser("Burke", "test-realm"), "bill-burke", "monica-burke");

        provider.getTransaction().begin();
        provider.addUser("bill-burke", "bill-burke", null, "test-realm2");
        provider.getTransaction().commit();

        Assert.assertEquals(1, provider.getUsers("test-realm2").size());
        assertUsers(provider.searchForUser("Burke", "test-realm2"), "bill-burke");
    }

    private static void assertUsers(List<User> users, String... expectedIds) {
        if (expectedIds == null) {
            expectedIds = new String[0];
        }

        String[] actualIds = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            actualIds[i] = users.get(i).getId();
        }

        Arrays.sort(actualIds);
        Arrays.sort(expectedIds);

        assertArrayEquals(expectedIds, actualIds);
    }

    public static void assertUser(User expected, User actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getEmail(), actual.getEmail());

        assertEquals(expected.getAttributes(), actual.getAttributes());
        assertEquals(expected.getRoleMappings(), actual.getRoleMappings());
    }

    public static void assertPassword(User user, String expectedPassword) {
        for (Credentials cred : user.getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                assertTrue("Invalid credentials", new Pbkdf2PasswordEncoder(cred.getSalt(), 1000).verify(expectedPassword, cred.getValue()));
                return;
            }
        }
        fail("Password credentials not found");
    }

    public static void assertTotp(User user, String expectedTotpSecret) {
        for (Credentials cred : user.getCredentials()) {
            if (cred.getType().equals(UserCredentialModel.TOTP)) {
                assertEquals(expectedTotpSecret, cred.getValue());
                return;
            }
        }
        fail("Totp credentials not found");
    }

}
