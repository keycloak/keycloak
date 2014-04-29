package org.keycloak.models.utils.reflection;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

/**
 * A {@link java.security.PrivilegedAction} that calls {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}
 */
public class SetAccessiblePrivilegedAction implements PrivilegedAction<Void> {

    private final AccessibleObject member;

    public SetAccessiblePrivilegedAction(AccessibleObject member) {
        this.member = member;
    }

    public Void run() {
        member.setAccessible(true);
        return null;
    }

}
