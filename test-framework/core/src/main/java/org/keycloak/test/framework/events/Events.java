package org.keycloak.test.framework.events;

import org.keycloak.events.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Events implements SysLogListener {

    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

    public Event poll() {
        try {
            return events.poll(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void onLog(SysLog sysLog) {
        Event event = EventParser.parse(sysLog);
        if (event != null) {
            events.add(event);
        }
    }
}
