package org.keycloak.test.tools;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("mail")
public class Mail {

    private GreenMail greenMail;

    @GET
    @Path("status")
    @Produces("application/json")
    public synchronized Status status() {
        return new Status(greenMail != null);
    }

    @GET
    @Path("start")
    @Produces("application/json")
    public synchronized Status start() {
        if (greenMail == null) {
            ServerSetup setup = new ServerSetup(3025, "localhost", "smtp");

            greenMail = new GreenMail(setup);
            try {
                greenMail.start();
            } catch (Throwable t) {
                greenMail = null;
                return new Status(false);
            }
        }

        return new Status(true);
    }

    @GET
    @Path("stop")
    @Produces("application/json")
    public synchronized Status stop() {
        if (greenMail != null) {
            greenMail.stop();
            greenMail = null;
        }

        return new Status(false);
    }

    @GET
    @Path("messages")
    @Produces("application/json")
    public synchronized List<Message> getMessages() throws Exception {
        List<Message> messages = new LinkedList<Message>();
        if (greenMail != null) {
            for (MimeMessage m : greenMail.getReceivedMessages()) {
                messages.add(new Message(m));
            }
        }
        return messages;
    }

    @Override
    protected void finalize() throws Throwable {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    public static class Status {

        private boolean started;

        public Status(boolean started) {
            this.started = started;
        }

        public boolean isStarted() {
            return started;
        }

    }

    public static class Message {

        private String from;
        private String to;
        private String subject;
        private String body;
        private Long date;

        public Message(MimeMessage m) throws Exception {
            from = m.getFrom()[0].toString();
            to = m.getRecipients(MimeMessage.RecipientType.TO)[0].toString();
            subject = m.getSubject();
            body = m.getContent().toString();
            date = m.getSentDate() != null ? m.getSentDate().getTime() : null;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }

        public Long getDate() {
            return date;
        }

    }

}
