package org.keycloak.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupModel extends RoleMapperModel {
    String getId();

    String getName();

    void setName(String name);

    /**
     * Set single value of specified attribute. Remove all other existing values
     *
     * @param name
     * @param value
     */
    void setSingleAttribute(String name, String value);

    void setAttribute(String name, List<String> values);

    void removeAttribute(String name);

    /**
     * @param name
     * @return null if there is not any value of specified attribute or first value otherwise. Don't throw exception if there are more values of the attribute
     */
    String getFirstAttribute(String name);

    /**
     * @param name
     * @return list of all attribute values or empty list if there are not any values. Never return null
     */
    List<String> getAttribute(String name);

    Map<String, List<String>> getAttributes();

    GroupModel getParent();
    String getParentId();
    Set<GroupModel> getSubGroups();

    /**
     * You must also call addChild on the parent group, addChild on RealmModel if there is no parent group
     *
     * @param group
     */
    void setParent(GroupModel group);

    /**
     * Automatically calls setParent() on the subGroup
     *
     * @param subGroup
     */
    void addChild(GroupModel subGroup);

    /**
     * Automatically calls setParent() on the subGroup
     *
     * @param subGroup
     */
    void removeChild(GroupModel subGroup);
}
