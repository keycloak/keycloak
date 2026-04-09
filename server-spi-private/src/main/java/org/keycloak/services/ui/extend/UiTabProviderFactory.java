package org.keycloak.services.ui.extend;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiTabProviderFactory<T> extends ComponentFactory<T, UiTabProvider> {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    @Override
    default Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("path", getPath());
        metadata.put("params", getParams());
        metadata.put("storageType", getStorageType().name());
        String endpoint = getEndpoint();
        if (endpoint != null) {
            metadata.put("endpoint", endpoint);
        }
        return metadata;
    }

    /**
     * Returns the path pattern for this tab.
     * The path can contain parameters like {@code :clientId} or {@code :userId}.
     *
     * @return the path pattern
     */
    String getPath();

    /**
     * Returns the parameter names used in the path.
     *
     * @return map of parameter names
     */
    Map<String, String> getParams();

    /**
     * Returns the storage type for this tab's data.
     * <p>
     * Override this method to change how form data is stored:
     * <ul>
     *   <li>{@link StorageType#COMPONENT} - Store as a Keycloak component (default)</li>
     *   <li>{@link StorageType#CLIENT} - Store on client attributes (requires clientId param)</li>
     *   <li>{@link StorageType#USER} - Store on user attributes (requires userId param)</li>
     *   <li>{@link StorageType#GROUP} - Store on group attributes (requires groupId param)</li>
     *   <li>{@link StorageType#IDENTITY_PROVIDER} - Store on IdP config (requires providerId param)</li>
     *   <li>{@link StorageType#REALM} - Store on realm attributes</li>
     *   <li>{@link StorageType#CUSTOM} - Use custom endpoint (requires {@link #getEndpoint()})</li>
     * </ul>
     *
     * @return the storage type
     */
    default StorageType getStorageType() {
        return StorageType.COMPONENT;
    }

    /**
     * Returns the custom endpoint path for {@link StorageType#CUSTOM} storage.
     * <p>
     * The endpoint should be implemented via
     * {@link org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider}.
     * The path is relative to {@code /admin/realms/{realm}/}.
     * <p>
     * Example: {@code "my-extension/clients/{clientId}/settings"}
     *
     * @return the endpoint path, or null if not using custom storage
     */
    default String getEndpoint() {
        return null;
    }
}
