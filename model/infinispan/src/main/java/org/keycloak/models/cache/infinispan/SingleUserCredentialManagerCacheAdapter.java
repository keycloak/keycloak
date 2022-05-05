package org.keycloak.models.cache.infinispan;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.SingleUserCredentialManager;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Alexander Schwartz
 */
public abstract class SingleUserCredentialManagerCacheAdapter implements SingleUserCredentialManager {

    private final SingleUserCredentialManager singleUserCredentialManager;

    protected SingleUserCredentialManagerCacheAdapter(SingleUserCredentialManager singleUserCredentialManager) {
        this.singleUserCredentialManager = singleUserCredentialManager;
    }

    public abstract void invalidateCacheForUser();

    @Override
    public boolean isValid(List<CredentialInput> inputs) {
        // validating a password might still update its hashes, similar logic might apply to OTP logic
        // instead of having each
        invalidateCacheForUser();
        return singleUserCredentialManager.isValid(inputs);
    }

    @Override
    public boolean updateCredential(CredentialInput input) {
        invalidateCacheForUser();
        return singleUserCredentialManager.updateCredential(input);
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        invalidateCacheForUser();
        singleUserCredentialManager.updateStoredCredential(cred);
    }

    @Override
    public CredentialModel createStoredCredential(CredentialModel cred) {
        invalidateCacheForUser();
        return singleUserCredentialManager.createStoredCredential(cred);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        invalidateCacheForUser();
        return singleUserCredentialManager.removeStoredCredentialById(id);
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        return singleUserCredentialManager.getStoredCredentialById(id);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsStream() {
        return singleUserCredentialManager.getStoredCredentialsStream();
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        return singleUserCredentialManager.getStoredCredentialsByTypeStream(type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(String name, String type) {
        return singleUserCredentialManager.getStoredCredentialByNameAndType(name, type);
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        invalidateCacheForUser();
        return singleUserCredentialManager.moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String userLabel) {
        invalidateCacheForUser();
        singleUserCredentialManager.updateCredentialLabel(credentialId, userLabel);
    }

    @Override
    public void disableCredentialType(String credentialType) {
        invalidateCacheForUser();
        singleUserCredentialManager.disableCredentialType(credentialType);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream() {
        return singleUserCredentialManager.getDisableableCredentialTypesStream();
    }

    @Override
    public boolean isConfiguredFor(String type) {
        return singleUserCredentialManager.isConfiguredFor(type);
    }

    @Override
    public boolean isConfiguredLocally(String type) {
        return singleUserCredentialManager.isConfiguredLocally(type);
    }

    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        return singleUserCredentialManager.getConfiguredUserStorageCredentialTypesStream();
    }

    @Override
    public CredentialModel createCredentialThroughProvider(CredentialModel model) {
        invalidateCacheForUser();
        return singleUserCredentialManager.createCredentialThroughProvider(model);
    }

}
