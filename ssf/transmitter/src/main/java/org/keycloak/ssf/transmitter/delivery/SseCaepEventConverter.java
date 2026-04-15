package org.keycloak.ssf.transmitter.delivery;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.ssf.event.token.SseCaepSecurityEventToken;
import org.keycloak.ssf.event.token.SsfSecurityEventToken;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SseCaepEventConverter {

    public static SseCaepSecurityEventToken convert(SsfSecurityEventToken ssfEventToken) {

        SseCaepSecurityEventToken sseCaepToken = new SseCaepSecurityEventToken();
        sseCaepToken.setJti(ssfEventToken.getJti());
        sseCaepToken.setIss(ssfEventToken.getIss());
        sseCaepToken.setAud(ssfEventToken.getAud());
        sseCaepToken.setIat(ssfEventToken.getIat());

        Map<String, Object> sseCaepEventData = new HashMap<>();
        for (String eventType : ssfEventToken.getEvents().keySet()) {

            Map<String, Object> eventData = new ObjectMapper().convertValue(ssfEventToken.getEvents().get(eventType), Map.class);
            Map<String, Object> adjustedEventData = new HashMap<>();
            adjustedEventData.put("reason_admin", eventData.get("reason_admin"));
            adjustedEventData.put("reason_user", eventData.get("reason_user"));
            adjustedEventData.put("event_timestamp", eventData.get("event_timestamp"));
            adjustedEventData.put("initiating_entity", eventData.get("initiating_entity"));

            adjustedEventData.put("credential_type", eventData.get("credential_type"));
            adjustedEventData.put("change_type", eventData.get("change_type"));

            var subjectMap = new HashMap<String, Object>();
            subjectMap.put("user", ssfEventToken.getSubjectId());
            adjustedEventData.put("subject", subjectMap);

            sseCaepEventData.put(eventType, adjustedEventData);
        }
        sseCaepToken.setEvents(sseCaepEventData);

        return sseCaepToken;
    }
}
