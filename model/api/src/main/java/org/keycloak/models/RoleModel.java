package org.keycloak.models;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public interface RoleModel {
    String getName();

    String getDescription();

    void setDescription(String description);

    String getId();

    void setName(String name);

    boolean isComposite();

    void addCompositeRole(RoleModel role);

    void removeCompositeRole(RoleModel role);

    Set<RoleModel> getComposites();

    RoleContainerModel getContainer();

    boolean hasRole(RoleModel role);
    
    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();
    String getFederationLink();
    void setFederationLink(String link);

}
