package org.keycloak.protocol.ssf.event;


import org.keycloak.protocol.ssf.event.types.GenericSsfEvent;
import org.keycloak.protocol.ssf.event.types.SsfEvent;
import org.keycloak.protocol.ssf.event.types.VerificationEvent;
import org.keycloak.protocol.ssf.event.types.caep.AssuranceLevelChange;
import org.keycloak.protocol.ssf.event.types.caep.CaepEvent;
import org.keycloak.protocol.ssf.event.types.caep.CredentialChange;
import org.keycloak.protocol.ssf.event.types.caep.DeviceComplianceChange;
import org.keycloak.protocol.ssf.event.types.caep.SessionEstablished;
import org.keycloak.protocol.ssf.event.types.caep.SessionPresented;
import org.keycloak.protocol.ssf.event.types.caep.SessionRevoked;
import org.keycloak.protocol.ssf.event.types.caep.TokenClaimsChanged;
import org.keycloak.protocol.ssf.event.types.risc.AccountCredentialChangeRequired;
import org.keycloak.protocol.ssf.event.types.risc.AccountDisabled;
import org.keycloak.protocol.ssf.event.types.risc.AccountEnabled;
import org.keycloak.protocol.ssf.event.types.risc.AccountPurged;
import org.keycloak.protocol.ssf.event.types.risc.CredentialCompromise;
import org.keycloak.protocol.ssf.event.types.risc.IdentifierChanged;
import org.keycloak.protocol.ssf.event.types.risc.IdentifierRecycled;
import org.keycloak.protocol.ssf.event.types.risc.OptIn;
import org.keycloak.protocol.ssf.event.types.risc.OptOutCancelled;
import org.keycloak.protocol.ssf.event.types.risc.OptOutEffective;
import org.keycloak.protocol.ssf.event.types.risc.OptOutInitiated;
import org.keycloak.protocol.ssf.event.types.risc.RecoveryActivated;
import org.keycloak.protocol.ssf.event.types.risc.RecoveryInformationChanged;
import org.keycloak.protocol.ssf.event.types.risc.RiscEvent;
import org.keycloak.protocol.ssf.event.types.scim.AsyncCompletionEvent;
import org.keycloak.protocol.ssf.event.types.scim.EventFeedAdded;
import org.keycloak.protocol.ssf.event.types.scim.EventFeedRemoved;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningActivatedEvent;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningCreatedEventFull;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningCreatedEventNotice;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningDeactivatedEvent;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningDeletedEvent;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningPatchEventFull;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningPatchEventNotice;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningPutEventFull;
import org.keycloak.protocol.ssf.event.types.scim.ProvisioningPutEventNotice;
import org.keycloak.protocol.ssf.event.types.scim.ScimEvent;

import javax.sound.midi.Patch;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityEvents {

    public final static Map<String, Class<? extends CaepEvent>> CAEP_EVENTS_TYPES;
    public final static Map<String, Class<? extends RiscEvent>> RISC_EVENTS_TYPES;
    public static final Map<String, Class<? extends ScimEvent>> SCIM_EVENTS_TYPES;

    static {
        var caepEventTypes = new HashMap<String, Class<? extends CaepEvent>>();
        List.of( //
                new AssuranceLevelChange(), //
                new CredentialChange(), //
                new DeviceComplianceChange(), //
                new SessionEstablished(), //
                new SessionPresented(), //
                new SessionRevoked(), //
                new TokenClaimsChanged() //
        ).forEach(caepEvent -> caepEventTypes.put(caepEvent.getEventType(), caepEvent.getClass()));
        CAEP_EVENTS_TYPES = Collections.unmodifiableMap(caepEventTypes);

        var riscEventTypes = new HashMap<String, Class<? extends RiscEvent>>();
        List.of( //
                new AccountCredentialChangeRequired(), //
                new AccountDisabled(), //
                new AccountEnabled(), //
                new AccountPurged(), //
                new CredentialCompromise(), //
                new IdentifierChanged(), //
                new IdentifierRecycled(), //
                new OptIn(), //
                new OptOutInitiated(), //
                new OptOutCancelled(), //
                new OptOutEffective(), //
                new RecoveryActivated(), //
                new RecoveryInformationChanged() //
        ).forEach(riscEvent -> riscEventTypes.put(riscEvent.getEventType(), riscEvent.getClass()));
        RISC_EVENTS_TYPES = Collections.unmodifiableMap(riscEventTypes);

        var scimEventTypes = new HashMap<String, Class<? extends ScimEvent>>();
        List.of(//
                new AsyncCompletionEvent(), //
                new EventFeedAdded(), //
                new EventFeedRemoved(), //
                new ProvisioningActivatedEvent(), //
                new ProvisioningCreatedEventFull(), //
                new ProvisioningCreatedEventNotice(), //
                new ProvisioningDeactivatedEvent(), //
                new ProvisioningDeletedEvent(), //
                new ProvisioningPatchEventFull(), //
                new ProvisioningPatchEventNotice(), //
                new ProvisioningPutEventFull(), //
                new ProvisioningPutEventNotice() //
        );
        SCIM_EVENTS_TYPES = Collections.unmodifiableMap(scimEventTypes);
    }

    public static boolean isCaepEvent(SsfEvent rawSsfEvent) {
        return CAEP_EVENTS_TYPES.containsKey(rawSsfEvent.getEventType());
    }

    public static boolean isRiscEvent(SsfEvent rawSsfEvent) {
        return RISC_EVENTS_TYPES.containsKey(rawSsfEvent.getEventType());
    }

    public static boolean isVerificationEventType(String eventType) {
        return VerificationEvent.TYPE.equals(eventType);
    }

    public static Class<? extends SsfEvent> getSecurityEventType(String eventType) {

        if (isVerificationEventType(eventType)) {
            return VerificationEvent.class;
        }

        var caepEventType = CAEP_EVENTS_TYPES.get(eventType);
        if (caepEventType != null) {
            return caepEventType;
        }

        var riscEventType = RISC_EVENTS_TYPES.get(eventType);
        if (riscEventType != null) {
            return riscEventType;
        }

        var scimEventType = SCIM_EVENTS_TYPES.get(eventType);
        if (scimEventType != null) {
            return scimEventType;
        }

        return GenericSsfEvent.class;
    }
}
