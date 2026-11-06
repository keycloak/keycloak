package org.keycloak.services.ui.extend;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;

public interface UiExtensionSupport {

    default List<String> getRequiredViewRoles() {
        return List.of(AdminRoles.VIEW_REALM);
    }

    default List<String> getRequiredManageRoles() {
        return List.of(AdminRoles.MANAGE_REALM);
    }

    /**
     * Navigation section for {@link UiPageProvider} entries: {@code manage}, {@code configure}, or {@code extensions}.
     */
    default String getNavSection() {
        return "configure";
    }

    /**
     * Optional localized label key for tabs. When {@code null}, the provider id is used.
     */
    default String getTabLabel() {
        return null;
    }

    /**
     * Returns form properties for the current context. Override to populate fields from realm, client, or other models.
     */
    default List<ProviderConfigProperty> getConfigProperties(KeycloakSession session, Map<String, String> contextParams) {
        if (this instanceof ConfiguredProvider configuredProvider) {
            return configuredProvider.getConfigProperties();
        }
        return Collections.emptyList();
    }

    default void putExtensionMetadata(Map<String, Object> metadata) {
        metadata.put("requiredViewRoles", getRequiredViewRoles());
        metadata.put("requiredManageRoles", getRequiredManageRoles());
        metadata.put("navSection", getNavSection());
        String tabLabel = getTabLabel();
        if (tabLabel != null) {
            metadata.put("tabLabel", tabLabel);
        }
    }
}
