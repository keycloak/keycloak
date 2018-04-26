package org.keycloak.performance.templates.idm;

import org.keycloak.performance.dataset.attr.AttributeMap;
import org.keycloak.performance.dataset.idm.Group;
import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.performance.templates.attr.StringListAttributeTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.GroupRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class GroupTemplate extends NestedEntityTemplate<Realm, Group, GroupRepresentation> {

    public static final String GROUPS_PER_REALM = "groupsPerRealm";

    public final int groupsPerRealm;
    public final int groupsTotal;

    public final GroupAttributeTemplate attributeTemplate;

    public GroupTemplate(RealmTemplate realmTemplate) {
        super(realmTemplate);
        this.groupsPerRealm = getConfiguration().getInt(GROUPS_PER_REALM, 0);
        this.groupsTotal = groupsPerRealm * realmTemplate.realms;
        this.attributeTemplate = new GroupAttributeTemplate();
    }

    public RealmTemplate realmTemplate() {
        return (RealmTemplate) getParentEntityTemplate();
    }

    @Override
    public Group newEntity(Realm parentEntity, int index) {
        return new Group(parentEntity, index);
    }

    @Override
    public void processMappings(Group group) {
        group.getRepresentation().setAttributes(new AttributeMap(new NestedEntityTemplateWrapperList<>(group, attributeTemplate)));
    }

    @Override
    public int getEntityCountPerParent() {
        return groupsPerRealm;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s, total: %s", GROUPS_PER_REALM, groupsPerRealm, groupsTotal));
        ValidateNumber.minValue(groupsPerRealm, 0);

        attributeTemplate.validateConfiguration();
    }

    public class GroupAttributeTemplate extends StringListAttributeTemplate<Group> {

        public static final String ATTRIBUTES_PER_GROUP = "attributesPerGroup";

        public final int attributesPerGroup;
        public final int attributesTotal;

        public GroupAttributeTemplate() {
            super(GroupTemplate.this);
            this.attributesPerGroup = getConfiguration().getInt(ATTRIBUTES_PER_GROUP, 0);
            this.attributesTotal = attributesPerGroup * groupsTotal;
        }

        @Override
        public int getEntityCountPerParent() {
            return attributesPerGroup;
        }

        @Override
        public void validateConfiguration() {
            logger().info(String.format("%s: %s", ATTRIBUTES_PER_GROUP, attributesPerGroup));
            ValidateNumber.minValue(attributesPerGroup, 0);
        }

    }

}
