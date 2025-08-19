package org.keycloak.federation.scim.core;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.AbstractKeycloakTransaction;

public class ScimUserStorageTransaction extends AbstractKeycloakTransaction {

    private final List<Runnable> runnables = new ArrayList<>();

    @Override
    protected void commitImpl() {
        runnables.forEach(Runnable::run);
    }

    @Override
    protected void rollbackImpl() {

    }

    public void execute(Runnable runnable) {
        this.runnables.add(runnable);
    }
}
