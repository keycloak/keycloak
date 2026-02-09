package org.keycloak.crypto.fips.test;

import org.keycloak.Config;
import org.keycloak.common.util.Environment;
import org.keycloak.credential.hash.AbstractPbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashSpi;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.rule.RunInThreadRule;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402Pbkdf2PasswordPaddingTest {

    private static final Logger logger = Logger.getLogger(FIPS1402SecureRandomTest.class);

    private static final int ITERATIONS = 27500;

    private static final int BC_FIPS_PADDING_LENGTH = 14;

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Rule
    public RunInThreadRule runInThread = new RunInThreadRule();

    private static boolean defaultBcFipsApprovedMode;

    @BeforeClass
    public static void checkBcFipsApproved() {
        defaultBcFipsApprovedMode = CryptoServicesRegistrar.isInApprovedOnlyMode();
    }

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
        Assert.assertEquals(defaultBcFipsApprovedMode, CryptoServicesRegistrar.isInApprovedOnlyMode());
    }

    @Test
    public void testShortPassword() {
        testPasswordVerification("short", false, BC_FIPS_PADDING_LENGTH);
    }

    @Test
    public void testLongPassword() {
        testPasswordVerification("someLongerPasswordThan14Chars", false, BC_FIPS_PADDING_LENGTH);
    }

    // Simulate the test for backwards compatibility - password created in non-approved mode should still work after server is restarted to approved mode
    @Test
    public void testShortPasswordWithSwitchToApprovedModel() {
        testPasswordVerification("short", true, BC_FIPS_PADDING_LENGTH);
    }

    // Simulate the test for backwards compatibility - password created in non-approved mode should still work after server is restarted to approved mode
    @Test
    public void testLongPasswordWithSwitchToApprovedModel() {
        testPasswordVerification("someLongerPasswordThan14Chars", true, BC_FIPS_PADDING_LENGTH);
    }

    @Test
    public void testShortPasswordWithSwitchToApprovedModelAndWithoutPadding() {
        try {
            testPasswordVerification("short", true, 0);
            Assert.fail("Password hashing should fail without padding in BCFIPS approved mode");
        } catch (FipsUnapprovedOperationError expectedError) {
            // Expected
        }
    }

    // Simulate the test for backwards compatibility - password created in non-approved mode should still work after server is restarted to approved mode
    @Test
    public void testLongPasswordWithSwitchToApprovedModelAndWithoutPadding() {
        testPasswordVerification("someLongerPasswordThan14Chars", true, 0);
    }


    private void testPasswordVerification(String password, boolean shouldEnableApprovedModeForVerification, int maxPaddingLength) {
        Pbkdf2Sha256PasswordHashProviderFactory factory = new Pbkdf2Sha256PasswordHashProviderFactory();

        System.setProperty("keycloak." + PasswordHashSpi.NAME + "." + Pbkdf2Sha256PasswordHashProviderFactory.ID + "." + AbstractPbkdf2PasswordHashProviderFactory.MAX_PADDING_LENGTH_PROPERTY,
                String.valueOf(maxPaddingLength));
        factory.init(Config.scope(PasswordHashSpi.NAME, Pbkdf2Sha256PasswordHashProviderFactory.ID));

        PasswordHashProvider pbkdf2HashProvider = factory.create(null);

        PasswordCredentialModel passwordCred = pbkdf2HashProvider.encodedCredential(password, ITERATIONS);
        logger.infof("After password credential created. BC FIPS approved mode: %b, password: %s", CryptoServicesRegistrar.isInApprovedOnlyMode(), password);

        if (shouldEnableApprovedModeForVerification) {
            CryptoServicesRegistrar.setApprovedOnlyMode(true);
        }

        logger.infof("Before password verification. BC FIPS approved mode: %b, password: %s", CryptoServicesRegistrar.isInApprovedOnlyMode(), password);
        assertThat(true, is(pbkdf2HashProvider.verify(password, passwordCred)));
    }


}
