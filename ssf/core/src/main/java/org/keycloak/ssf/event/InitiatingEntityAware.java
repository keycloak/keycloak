package org.keycloak.ssf.event;

/**
 * Implemented by event types that carry the {@code initiating_entity} claim.
 *
 * <p>The claim is defined by CAEP 1.0 as a profile-common optional claim, so
 * {@link org.keycloak.ssf.event.caep.CaepEvent} declares it for the whole CAEP
 * family. Individual events from other profiles may also carry it — Keycloak's
 * RISC account-state events do, because the distinction between an
 * administrator disabling an account and the brute-force policy engine doing so
 * is information receivers act on. Those events declare the field themselves
 * rather than inheriting it, since RISC does not define it profile-wide.
 *
 * <p>This interface exists so helpers that only need to stamp the claim (see
 * {@code SecurityEventTokenMapper#applyInitiatingEntity}) can accept both
 * families without widening {@link SsfEvent}, which deliberately carries only
 * SET member semantics.
 */
public interface InitiatingEntityAware {

    InitiatingEntity getInitiatingEntity();

    void setInitiatingEntity(InitiatingEntity initiatingEntity);
}
