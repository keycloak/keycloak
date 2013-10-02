package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RoleModel {
    String getName();

    String getDescription();

    void setDescription(String description);

    String getId();

    void setName(String name);
}
