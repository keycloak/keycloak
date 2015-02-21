package org.keycloak.models;

import java.util.LinkedList;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmListenerHelper {
    protected LinkedList<RealmProvider.RealmCreationListener> list = new LinkedList<RealmProvider.RealmCreationListener>();

    public void registerListener(RealmProvider.RealmCreationListener listener) {
        synchronized (list) {
            list.add(listener);
        }
    }

    public void unregisterListener(RealmProvider.RealmCreationListener listener) {
        synchronized (list) {
            list.remove(listener);
        }
    }

    public void executeCreationListeners(RealmModel realm) {
        synchronized (list) {
            for (RealmProvider.RealmCreationListener listener : list) {
                listener.created(realm);
            }
        }
    }
}
