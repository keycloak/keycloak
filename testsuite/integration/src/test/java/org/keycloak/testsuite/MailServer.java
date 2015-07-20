package org.keycloak.testsuite;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

public class MailServer {

    public static void main(String[] args) throws Exception {
        ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

        GreenMail greenMail = new GreenMail(setup);
        greenMail.start();

        System.out.println("Started mail server (localhost:3025)");
        System.out.println();
        
        while (true) {
            int c = greenMail.getReceivedMessages().length;

            if (greenMail.waitForIncomingEmail(Long.MAX_VALUE, c + 1)) {
                MimeMessage message = greenMail.getReceivedMessages()[c++];
                System.out.println("-------------------------------------------------------");
                System.out.println("Received mail to " + message.getRecipients(RecipientType.TO)[0]);
                if (message.getContent() instanceof MimeMultipart) {
                    MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                    for (int i = 0; i < mimeMultipart.getCount(); i++) {
                        System.out.println("----");
                        System.out.println(mimeMultipart.getBodyPart(i).getContentType() + ":");
                        System.out.println();
                        System.out.println(mimeMultipart.getBodyPart(i).getContent());
                    }
                } else {
                    System.out.println();
                    System.out.println(message.getContent());
                }
                System.out.println("-------------------------------------------------------");
            }
        }
    }

}
