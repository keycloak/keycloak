package org.keycloak.services.ui.extend;

/**
 * Defines the storage backend for UI tab data.
 * <p>
 * When implementing a {@link UiTabProvider}, you can specify how the form data
 * should be stored. The default is {@link #COMPONENT}, which stores data as a
 * Keycloak component. Other options allow storing data directly on existing
 * Keycloak entities like clients, users, groups, or identity providers.
 */
public enum StorageType {

    /**
     * Store data as a Keycloak component (default behavior).
     * Data is saved using the components API.
     */
    COMPONENT,

    /**
     * Store data on a client's attributes.
     * Requires 'clientId' parameter in the URL.
     */
    CLIENT,

    /**
     * Store data on a user's attributes.
     * Requires 'userId' parameter in the URL.
     */
    USER,

    /**
     * Store data on a group's attributes.
     * Requires 'groupId' parameter in the URL.
     */
    GROUP,

    /**
     * Store data on an identity provider's config.
     * Requires 'providerId' parameter in the URL.
     */
    IDENTITY_PROVIDER,

    /**
     * Use a custom endpoint for storage.
     * Requires implementing an {@link org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider}
     * and specifying the endpoint path via {@link UiTabProviderFactory#getEndpoint()}.
     */
    CUSTOM
}
