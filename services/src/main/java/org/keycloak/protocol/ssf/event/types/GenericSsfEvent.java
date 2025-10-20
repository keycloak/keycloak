package org.keycloak.protocol.ssf.event.types;

public class GenericSsfEvent extends SsfEvent {

    public GenericSsfEvent() {
        super(null);
    }

    @Override
    public String toString() {
        return "GenericSecurityEvent{" +
               "subjectId=" + subjectId +
               ", eventType='" + eventType + '\'' +
               ", eventTimestamp=" + eventTimestamp +
               ", initiatingEntity=" + initiatingEntity +
               ", reasonAdmin=" + reasonAdmin +
               ", reasonUser=" + reasonUser +
               ", attributes=" + attributes +
               '}';
    }
}
