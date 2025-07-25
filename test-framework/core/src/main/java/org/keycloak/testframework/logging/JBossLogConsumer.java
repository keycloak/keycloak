package org.keycloak.testframework.logging;

import org.jboss.logging.Logger;
import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

public class JBossLogConsumer implements Consumer<OutputFrame> {

    private final Logger logger;

    public JBossLogConsumer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        OutputFrame.OutputType type = outputFrame.getType();
        switch (type) {
            case STDOUT:
                logger.debug(outputFrame.getUtf8StringWithoutLineEnding());
                break;
            case STDERR:
                logger.warn(outputFrame.getUtf8StringWithoutLineEnding());
                break;
        }
    }

}
