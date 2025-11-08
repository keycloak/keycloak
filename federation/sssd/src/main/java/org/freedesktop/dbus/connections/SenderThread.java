package org.freedesktop.dbus.connections;

import java.util.concurrent.LinkedBlockingQueue;

import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenderThread extends Thread {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean      terminate;

    private final LinkedBlockingQueue<Message> outgoingQueue = new LinkedBlockingQueue<>();

    private final AbstractConnection abstractConnection;

    SenderThread(AbstractConnection _abstractConnection) {
        abstractConnection = _abstractConnection;
        setName("DBUS Sender Thread");
    }

    public void terminate() {
        terminate = true;
        interrupt();
    }

    public LinkedBlockingQueue<Message> getOutgoingQueue() {
        return outgoingQueue;
    }

    @Override
    public void run() {
        Message m;

        logger.trace("Monitoring outbound queue");
        // block on the outbound queue and send from it
        while (!terminate) {
            try {
                m = outgoingQueue.take();
                if (m != null) {
                    abstractConnection.sendMessage(m);
                }
            } catch (InterruptedException _ex) {
                if (!terminate) { // if terminate is true, shutdown was requested, do not log that
                    logger.warn("Interrupted while waiting for a message to send", _ex);
                }
            }
        }

        logger.debug("Flushing outbound queue and quitting");
        // flush the outbound queue before disconnect.
        while (!outgoingQueue.isEmpty()) {
            Message poll = outgoingQueue.poll();
            if (poll != null) {
                abstractConnection.sendMessage(outgoingQueue.poll());
            } else {
                break;
            }
        }
    }
}
