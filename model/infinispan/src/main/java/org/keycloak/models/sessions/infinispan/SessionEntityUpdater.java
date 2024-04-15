package org.keycloak.models.sessions.infinispan;

/**
 * An updated interface for Infinispan cache.
 * <p>
 * When the entity is changed, the new entity must be written (or removed) into the Infinispan cache.
 * The methods {@link #onEntityUpdated()} and {@link #onEntityRemoved()} signals the entity has changed.
 *
 * @param <T> The entity type.
 */
public interface SessionEntityUpdater<T> {

    /**
     * @return The entity tracked by this {@link SessionEntityUpdater}.
     * It does not fetch the value from the Infinispan cache and uses a local copy.
     */
    T getEntity();

    /**
     * Signals that the entity was updated, and the Infinispan cache needs to be updated.
     */
    void onEntityUpdated();

    /**
     * Signals that the entity was removed, and the Infinispan cache needs to be updated.
     */
    void onEntityRemoved();

}
