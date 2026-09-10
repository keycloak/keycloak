package org.keycloak.models.utils;

import java.util.function.Function;

import org.keycloak.models.UserModel;

public class StorageUnavailableUserModelDelegate extends ReadOnlyUserModelDelegate {

    public StorageUnavailableUserModelDelegate(UserModel delegate, Function<String, RuntimeException> exceptionCreator) {
        super(delegate, false, exceptionCreator);
    }
}
