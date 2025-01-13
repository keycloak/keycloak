package org.keycloak.testframework.events;

import org.keycloak.events.admin.AdminEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AdminEvents implements SysLogListener{

    private final BlockingQueue<AdminEvent> adminEvents = new LinkedBlockingQueue<>();

    public AdminEvent poll() {
        try {
            return adminEvents.poll(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void clear() {
        adminEvents.clear();
    }

    @Override
    public void onLog(SysLog sysLog) {
        AdminEvent adminEvent = AdminEventsParser.parse(sysLog);
        if (adminEvent != null) {
            adminEvents.add(adminEvent);
        }
    }
}
