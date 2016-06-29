package org.keycloak.testsuite.util;

import org.jboss.logging.Logger;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;

import java.io.InputStream;

public class MessageHandlerImpl implements MessageHandler {
    MessageContext context;

    private static final Logger log = Logger.getLogger(MessageHandlerImpl.class);

    MessageHandlerImpl(MessageContext context) {
        this.context = context;
    }

    @Override
    public void from(String from) {
        log.info("FROM: ${from}");
    }

    @Override
    public void recipient(String recipient) {
        log.info("RECIPIENT: ${recipient}");
    }

    @Override
    public void data(InputStream data) {
        log.info("DATA");
    }

    @Override
    public void done() {
        log.info("DONE");
    }
}
