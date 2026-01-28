package org.keycloak.testframework.events;

public interface SysLogListener {

    void onLog(SysLog sysLog);

}
