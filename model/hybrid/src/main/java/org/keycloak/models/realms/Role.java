package org.keycloak.models.realms;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Role {

    String getId();

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean isComposite();

    void addCompositeRole(Role role);

    void removeCompositeRole(Role role);

    Set<Role> getComposites();

    RoleContainer getContainer();

}
