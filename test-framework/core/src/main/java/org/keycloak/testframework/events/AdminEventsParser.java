package org.keycloak.testframework.events;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

public class AdminEventsParser {

    private AdminEventsParser() {
    }

    public static AdminEvent parse(SysLog sysLog) {
        if (!sysLog.getCategory().equals("org.keycloak.events")) {
            return null;
        }

        String message = sysLog.getMessage().substring(sysLog.getMessage().indexOf(')') + 1).trim();

        if (!message.startsWith("operationType=")) {
            return null;
        }

        String[] split = message.split(", ");

        Map<String, String> eventMap = new HashMap<>();
        for (String s : split) {
            String[] split1 = s.split("=");
            eventMap.put(split1[0], split1[1].substring(1, split1[1].length() - 1));
        }

        AdminEvent adminEvent = new AdminEvent();
        adminEvent.setTime(sysLog.getTimestamp().getTime() / 1000);
        adminEvent.setAuthDetails(new AuthDetails());

        for (Map.Entry<String, String> e : eventMap.entrySet()) {
            switch (e.getKey()) {
                case "operationType":
                    adminEvent.setOperationType(OperationType.valueOf(e.getValue()));
                    break;
                case "realmId":
                    adminEvent.setRealmId(e.getValue());
                    adminEvent.getAuthDetails().setRealmId(e.getValue());
                    break;
                case "realmName":
                    adminEvent.getAuthDetails().setRealmName(e.getValue());
                    break;
                case "clientId":
                    adminEvent.getAuthDetails().setClientId(e.getValue());
                    break;
                case "userId":
                    adminEvent.getAuthDetails().setUserId(e.getValue());
                    break;
                case "ipAddress":
                    adminEvent.getAuthDetails().setIpAddress(e.getValue());
                    break;
                case "resourceType":
                    adminEvent.setResourceType(ResourceType.valueOf(e.getValue()));
                    break;
                case "resourcePath":
                    adminEvent.setResourcePath(e.getValue());
                    break;
                case "error":
                    adminEvent.setError(e.getValue());
                    break;
                default:
                    break;
            }
        }

        return adminEvent;
    }
}
