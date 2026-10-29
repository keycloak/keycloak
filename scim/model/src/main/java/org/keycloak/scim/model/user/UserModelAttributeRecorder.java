package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

public class UserModelAttributeRecorder extends UserModelDelegate {

    private final Map<String, List<String>> attributes;

    public UserModelAttributeRecorder(UserModel delegate, Map<String, List<String>> attributes) {
        super(delegate);
        this.attributes = attributes;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        attributes.put(name, value == null ? List.of() : List.of(value));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        attributes.put(name, values == null ? List.of() : new ArrayList<>(values));
    }

    @Override
    public void removeAttribute(String name) {
        attributes.put(name, List.of());
    }

    @Override
    public void setUsername(String username) {
        setSingleAttribute(UserModel.USERNAME, username);
    }

    @Override
    public void setEmail(String email) {
        setSingleAttribute(UserModel.EMAIL, email);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(UserModel.FIRST_NAME, firstName);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(UserModel.LAST_NAME, lastName);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (attributes.containsKey(name)) {
            return attributes.get(name).stream();
        }
        return super.getAttributeStream(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (attributes.containsKey(name)) {
            List<String> values = attributes.get(name);
            return values.isEmpty() ? null : values.get(0);
        }
        return super.getFirstAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public String getUsername() {
        return attributes.containsKey(UserModel.USERNAME) ? getFirstAttribute(UserModel.USERNAME) : super.getUsername();
    }

    @Override
    public String getEmail() {
        return attributes.containsKey(UserModel.EMAIL) ? getFirstAttribute(UserModel.EMAIL) : super.getEmail();
    }

    @Override
    public String getFirstName() {
        return attributes.containsKey(UserModel.FIRST_NAME) ? getFirstAttribute(UserModel.FIRST_NAME) : super.getFirstName();
    }

    @Override
    public String getLastName() {
        return attributes.containsKey(UserModel.LAST_NAME) ? getFirstAttribute(UserModel.LAST_NAME) : super.getLastName();
    }
}
