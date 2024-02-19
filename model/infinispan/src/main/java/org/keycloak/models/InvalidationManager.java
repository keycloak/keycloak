package org.keycloak.models;

import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class manages invalidations for the cache and makes the behavior accessible to any cache provider.
 * Prefix invalidations prioritize the O(N) upfront cost of finding a more generic prefix entry to reduce the
 * number of iterations on the key-value store for removal
 */
public class InvalidationManager {

    Set<String> prefixInvalidations;
    Set<String> modelInvalidations;
    Set<InvalidationEvent> invalidationEvents; // these invalidation events get sent across the cluster

    public InvalidationManager() {
        prefixInvalidations = new HashSet<>();
        modelInvalidations = new HashSet<>();
        invalidationEvents = new HashSet<>();
    }

    /**
     * Adds prefix to invalidation set if there is no prefix in the set more generic than it
     * @param prefix The prefix to be added
     */
    public void addPrefixInvalidation(String prefix) {
        Iterator<String> prefixIter = prefixInvalidations.iterator();

        boolean isMostGeneric = true;
        while(prefixIter.hasNext()) {
            String exitingPrefix = prefixIter.next();
            if(prefix.startsWith(exitingPrefix)) {
                isMostGeneric = false;
                break;
            }
            if (exitingPrefix.startsWith(prefix)) {
                prefixIter.remove();
            }
        }
        if(isMostGeneric) {
            prefixInvalidations.add(prefix);
        }
    }

    public void addModelInvalidation(String model) {
        modelInvalidations.add(model);
    }

    public void addInvalidationEvent(InvalidationEvent event) {
        invalidationEvents.add(event);
    }

    /**
     * Checks if a given prefix is invalidated in the current cache session
     * @param key The prefix to check
     * @return True if the key is in the list or any key in the list is also a prefix for the provided key
     */
    public boolean isPrefixInvalidated(String key) {
        return prefixInvalidations.stream().anyMatch(key::startsWith);
    }

    public boolean isModelInvalidated(String modelId) {
        return modelInvalidations.contains(modelId);
    }

    public Set<InvalidationEvent> getInvalidationEvents() {
        return invalidationEvents;
    }

    public Set<String> getPrefixInvalidations() {
        return prefixInvalidations;
    }

    public Set<String> getModelInvalidations() {
        return modelInvalidations;
    }
}
