package org.keycloak.testsuite.util;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;


public class MessageHandlerFactoryImpl implements MessageHandlerFactory {

    MimeMessage message;

    public MessageHandler create(MessageContext ctx) {
        return new Handler(ctx);
    }

    class Handler implements MessageHandler {
        MessageContext ctx;



        public Handler(MessageContext ctx) {
            this.ctx = ctx;
        }

        public void from(String from) throws RejectException {
            System.out.println("FROM:" + from);
        }

        public void recipient(String recipient) throws RejectException {
            System.out.println("RECIPIENT:" + recipient);
        }

        public void data(InputStream data) throws IOException {
            String rawMail = this.convertStreamToString(data);

            Session session = Session.getDefaultInstance(new Properties());
            InputStream is = new ByteArrayInputStream(rawMail.getBytes());
            try
            {
                message = new MimeMessage(session, is);
                setMessage(message);
            }
            catch (MessagingException e)
            {
                e.printStackTrace();
            }
        }

        public void done() {
            System.out.println("Finished");
        }

        public String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }



    }

    public MimeMessage getMessage(){
        return message;
    }

    public  void setMessage(MimeMessage msg){
        this.message = msg;
    }
}
