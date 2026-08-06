package org.keycloak.ssf.event.caep;

import java.util.Map;

import org.keycloak.ssf.event.SsfEvent;

/**
 * Generic CaepEvent.
 *
 * See: https://openid.net/specs/openid-caep-1_0-final.html
 */
public abstract class CaepEvent extends SsfEvent {

    public CaepEvent(String type) {
        super(type);
    }

    @Override
    public Map<String, Object> createAdminDetails() {
        Map<String, Object> adminRep = super.createAdminDetails();
        if (eventTimestamp != null) {
            adminRep.put("event_timestamp", eventTimestamp);
        }
        if (initiatingEntity != null) {
            adminRep.put("initiating_entity", initiatingEntity);
        }
        // excluding reasonAdmin and reasonUser to avoid exposing PII here
        return adminRep;
    }
}
