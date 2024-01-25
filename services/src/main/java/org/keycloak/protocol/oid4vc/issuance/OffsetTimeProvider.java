package org.keycloak.protocol.oid4vc.issuance;

import org.keycloak.common.util.Time;

/**
 * Implementation of the {@link TimeProvider} that delegates calls to the common {@link Time} class.
 */
public class OffsetTimeProvider implements TimeProvider {

    @Override
    public int currentTime() {
        return Time.currentTime();
    }

    @Override
    public long currentTimeMillis() {
        return Time.currentTimeMillis();
    }
}
