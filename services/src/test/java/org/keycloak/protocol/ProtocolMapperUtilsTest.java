package org.keycloak.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import java.util.*;

import static org.junit.Assert.*;

public class ProtocolMapperUtilsTest {
    @Test
    public void canGetStringAttributesThatAreDefinedInUserModelInterface() {
        UserModel user = new SimpleUserModel();

        Assert.assertEquals(user.getId(),
                ProtocolMapperUtils.getUserModelValue(user, "id"));
        Assert.assertEquals(user.getUsername(),
                ProtocolMapperUtils.getUserModelValue(user, "username"));
        Assert.assertEquals(user.getFirstName(),
                ProtocolMapperUtils.getUserModelValue(user, "firstName"));
        Assert.assertEquals(user.getLastName(),
                ProtocolMapperUtils.getUserModelValue(user, "lastName"));
        Assert.assertEquals(user.getEmail(),
                ProtocolMapperUtils.getUserModelValue(user, "email"));
    }

    @Test
    public void augmentedFieldsCanBeExtracted() {
        AugmentedUserModel user = new AugmentedUserModel();

        Assert.assertEquals(user.getAugmentedProperty(),
                ProtocolMapperUtils.getUserModelValue(user, "augmentedProperty"));

        Assert.assertEquals(String.format("%s", user.isAugmented()),
                ProtocolMapperUtils.getUserModelValue(user, "augmented"));

    }

    @Test
    public void nonExistingPropertyMapToNull() {
        UserModel user = new SimpleUserModel();

        Assert.assertNull(ProtocolMapperUtils.getUserModelValue(user, "nonExistingProperty"));
    }

    @Test
    public void otherTypesAreMappedToString() {
        UserModel user = new SimpleUserModel();

        Assert.assertEquals(user.getCreatedTimestamp().toString(),
                ProtocolMapperUtils.getUserModelValue(user, "createdTimestamp"));
    }

    @Test
    public void isAccessorsAreUsedIfGetAccessorNotDefined() {
        UserModel user = new SimpleUserModel();

        Assert.assertEquals(String.format("%s", user.isEnabled()),
                ProtocolMapperUtils.getUserModelValue(user, "enabled"));
    }

    private class AugmentedUserModel extends SimpleUserModel {
        private static final String EXPECTED_AUGMENTED_PROPERTY = "augmented";

        public String getAugmentedProperty() {
            return EXPECTED_AUGMENTED_PROPERTY;
        }

        public boolean isAugmented() {
            return true;
        }
    }

    private class SimpleUserModel implements UserModel {
        private static final String EXPECTED_FIRST_NAME = "John";
        private static final String EXPECTED_LAST_NAME = "Doe";
        private static final String EXPECTED_EMAIL = "john.doe@example.com";
        private static final String EXPECTED_ID = "id123";
        private static final String EXPECTED_USERNAME = "johndoe";

        @Override
        public String getId() {
            return EXPECTED_ID;
        }

        @Override
        public String getUsername() {
            return EXPECTED_USERNAME;
        }

        @Override
        public String getFirstName() {
            return EXPECTED_FIRST_NAME;
        }

        @Override
        public String getLastName() {
            return EXPECTED_LAST_NAME;
        }

        @Override
        public String getEmail() {
            return EXPECTED_EMAIL;
        }

        @Override
        public Set<RoleModel> getRealmRoleMappings() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<RoleModel> getClientRoleMappings(ClientModel app) {
            return Collections.EMPTY_SET;
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return false;
        }

        @Override
        public void grantRole(RoleModel role) {
        }

        @Override
        public Set<RoleModel> getRoleMappings() {
            return Collections.EMPTY_SET;
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
        }

        @Override
        public void setUsername(String username) {
        }

        @Override
        public Long getCreatedTimestamp() {
            return 0L;
        }

        @Override
        public void setCreatedTimestamp(Long timestamp) {
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public void setSingleAttribute(String name, String value) {
        }

        @Override
        public void setAttribute(String name, List<String> values) {
        }

        @Override
        public void removeAttribute(String name) {
        }

        @Override
        public String getFirstAttribute(String name) {
            return null;
        }

        @Override
        public List<String> getAttribute(String name) {
            return Collections.EMPTY_LIST;
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public Set<String> getRequiredActions() {
            return Collections.EMPTY_SET;
        }

        @Override
        public void addRequiredAction(String action) {
        }

        @Override
        public void removeRequiredAction(String action) {
        }

        @Override
        public void addRequiredAction(RequiredAction action) {
        }

        @Override
        public void removeRequiredAction(RequiredAction action) {
        }

        @Override
        public void setFirstName(String firstName) {
        }

        @Override
        public void setLastName(String lastName) {
        }

        @Override
        public void setEmail(String email) {
        }

        @Override
        public boolean isEmailVerified() {
            return true;
        }

        @Override
        public void setEmailVerified(boolean verified) {
        }

        @Override
        public Set<GroupModel> getGroups() {
            return Collections.EMPTY_SET;
        }

        @Override
        public void joinGroup(GroupModel group) {
        }

        @Override
        public void leaveGroup(GroupModel group) {
        }

        @Override
        public boolean isMemberOf(GroupModel group) {
            return false;
        }

        @Override
        public String getFederationLink() {
            return null;
        }

        @Override
        public void setFederationLink(String link) {
        }

        @Override
        public String getServiceAccountClientLink() {
            return null;
        }

        @Override
        public void setServiceAccountClientLink(String clientInternalId) {
        }
    }
}
