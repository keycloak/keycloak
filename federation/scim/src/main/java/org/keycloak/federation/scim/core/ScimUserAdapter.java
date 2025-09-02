package org.keycloak.federation.scim.core;

import java.util.List;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

class ScimUserAdapter extends UserModelDelegate {

    private boolean dirty;

    ScimUserAdapter(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username);
        markDirty();
    }

    @Override
    public void setEmail(String email) {
        super.setEmail(email);
        markDirty();
    }

    @Override
    public void setFirstName(String firstName) {
        super.setFirstName(firstName);
        markDirty();
    }

    @Override
    public void setLastName(String lastName) {
        super.setLastName(lastName);
        markDirty();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        markDirty();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        super.setSingleAttribute(name, value);
        markDirty();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        super.setAttribute(name, values);
        markDirty();
    }

    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        dirty = true;
    }
}
