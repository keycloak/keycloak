package org.keycloak.testsuite.rule;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.internet.MimeMessage;

import org.junit.rules.ExternalResource;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

public class GreenMailRule extends ExternalResource {

    private GreenMail greenMail;

    private Properties originalProperties = new Properties();

    @Override
    protected void before() throws Throwable {
        Iterator<Entry<Object, Object>> itr = System.getProperties().entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Object, Object> e = itr.next();
            if (((String) e.getKey()).startsWith("keycloak.mail")) {
                originalProperties.put(e.getKey(), e.getValue());
                itr.remove();
            }
        }
        
        System.setProperty("keycloak.mail.smtp.from", "auto@keycloak.org");
        System.setProperty("keycloak.mail.smtp.host", "localhost");
        System.setProperty("keycloak.mail.smtp.port", "3025");
        
        ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

        greenMail = new GreenMail(setup);
        greenMail.start();
    }

    @Override
    protected void after() {
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

        System.getProperties().remove("keycloak.mail.smtp.from");
        System.getProperties().remove("keycloak.mail.smtp.host");
        System.getProperties().remove("keycloak.mail.smtp.port");
        System.getProperties().putAll(originalProperties);
    }

    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

}
