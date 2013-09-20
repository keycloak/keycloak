package org.keycloak.services.email;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class EmailSenderTest {

    private GreenMail greenMail;

    @Before
    public void before() {
        ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();

        System.setProperty("keycloak.mail.smtp.from", "auto@keycloak.org");
        System.setProperty("keycloak.mail.smtp.host", "localhost");
        System.setProperty("keycloak.mail.smtp.port", "3025");
    }

    @After
    public void after() throws InterruptedException {
        if (greenMail != null) {
            // Suppress error from GreenMail on shutdown
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (!(e.getCause() instanceof SocketException && t.getClass().getName()
                            .equals("com.icegreen.greenmail.smtp.SmtpHandler"))) {
                        System.err.print("Exception in thread \"" + t.getName() + "\" ");
                        e.printStackTrace(System.err);
                    }
                }
            });

            greenMail.stop();
        }
    }

    @Test
    public void sendMail() throws AddressException, MessagingException, IOException {
        EmailSender emailSender = new EmailSender();
        emailSender.send("test@test.com", "Test subject", "Test body");

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        Assert.assertEquals(1, receivedMessages.length);

        MimeMessage msg = receivedMessages[0];
        Assert.assertEquals(1, msg.getFrom().length);
        Assert.assertEquals("auto@keycloak.org", msg.getFrom()[0].toString());
        Assert.assertEquals("Test subject", msg.getSubject());
        Assert.assertEquals("Test body", ((String) msg.getContent()).trim());
    }

}
