package org.keycloak.testframework.events;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;

public class EventParser {

    private EventParser() {
    }

    public static Event parse(SysLog sysLog) {
        if (!sysLog.getCategory().equals("org.keycloak.events")) {
            return null;
        }

        String message = sysLog.getMessage().substring(sysLog.getMessage().indexOf(')') + 1).trim();

        if (!message.startsWith("type=")) {
            return null;
        }

        String[] split = message.split(", ");

        Map<String, String> eventMap = new HashMap<>();
        for (String s : split) {
            String[] split1 = s.split("=");
            eventMap.put(split1[0], split1[1].substring(1, split1[1].length() - 1));
        }

        Event event = new Event();
        event.setTime(sysLog.getTimestamp().getTime() / 1000);
        event.setDetails(new HashMap<>());

        for (Map.Entry<String, String> e : eventMap.entrySet()) {
            switch (e.getKey()) {
                case "type":
                    event.setType(EventType.valueOf(e.getValue()));
                    break;
                case "clientId":
                    event.setClientId(e.getValue());
                    break;
                case "realmId":
                    event.setRealmId(e.getValue());
                    break;
                case "sessionId":
                    event.setSessionId(e.getValue());
                    break;
                case "ipAddress":
                    event.setIpAddress(e.getValue());
                default:
                    event.getDetails().put(e.getKey(), e.getValue());
                    break;
            }
        }

        return event;
    }

}
