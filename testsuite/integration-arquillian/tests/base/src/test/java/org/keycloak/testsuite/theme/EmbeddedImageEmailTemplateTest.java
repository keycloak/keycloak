package org.keycloak.testsuite.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.actions.ActionUtil;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.MailUtils.EmailBody;
import org.keycloak.testsuite.util.UserBuilder;

import com.google.common.io.ByteStreams;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class EmbeddedImageEmailTemplateTest extends AbstractTestRealmKeycloakTest {

    public static final String EMAIL_THEME_NAME = "embedded-images";
    public static final String TEST_IMAGE_PATH = "/theme/embedded-images/email/resources/logo.png";

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPage loginPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setVerifyEmail(Boolean.TRUE);
        testRealm.setEmailTheme(EMAIL_THEME_NAME);
        ActionUtil.findUserInRealmRep(testRealm, "test-user@localhost").setEmailVerified(Boolean.FALSE);
    }

    @Before
    public void before() {
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create()
                .enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost").build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }

    @Test
    public void testEmbeddedImageInEmail() throws IOException {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        verifyEmailPage.assertCurrent();

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        EmailBody body = MailUtils.getBody(greenMail.getLastReceivedMessage());

        Pattern cidPattern = Pattern.compile("src=\"cid:([^\"]+)\"");
        Matcher cidMatcher = cidPattern.matcher(body.getHtml());
        while (cidMatcher.find()) {
            String cid = "<" + cidMatcher.group(1) + ">";
            Assert.assertTrue("Email body should contain the referenced image", body.getEmbedded().containsKey(cid));
            Object content = body.getEmbedded().get(cid);
            Assert.assertTrue("Image part content should be a stream", content instanceof InputStream);
            byte[] contentAsBytes = ByteStreams.toByteArray((InputStream) content);
            byte[] logoAsBytes = ByteStreams.toByteArray(getClass().getResourceAsStream(TEST_IMAGE_PATH));
            Assert.assertArrayEquals("Image part and logo.png should match", logoAsBytes, contentAsBytes);
        }
    }
}
