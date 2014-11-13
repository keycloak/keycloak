package org.keycloak.adapters.tomcat;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class SimpleGroup extends SimplePrincipal implements Group {
    private static final long serialVersionUID = 3273437693505893786L;
    private final Set<Principal> members = new HashSet<Principal>();

    /**
     * Creates a new group with the given name.
     * @param name Group name.
     */
    public SimpleGroup(final String name) {
        super(name);
    }

    public boolean addMember(final Principal user) {
        return this.members.add(user);
    }

    public boolean isMember(final Principal member) {
        return this.members.contains(member);
    }

    public Enumeration<? extends Principal> members() {
        return Collections.enumeration(this.members);
    }

    public boolean removeMember(final Principal user) {
        return this.members.remove(user);
    }
    
    public String toString() {
        return super.toString() + ": " + members.toString();
    }

}
