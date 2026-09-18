package org.keycloak.services.ui.extend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiPageProviderFactory<T> extends ComponentFactory<T, UiPageProvider>, UiExtensionSupport {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    /**
     * Fields shown in the page list. When empty, the Admin Console falls back to
     * the first three configured properties.
     */
    default List<String> getDisplayFields() {
        return List.of();
    }

    /**
     * When {@code true}, the page detail view renders declarative tabs using {@link #getDetailTabPath()}.
     */
    default boolean supportsDetailTabs() {
        return false;
    }

    /**
     * Route pattern for detail tabs. Must match the Admin Console page detail route
     * ({@code /:realm/page-section/:providerId/:id/:tab?}); only the optional tab
     * segment should be customized.
     */
    default String getDetailTabPath() {
        return "/:realm/page-section/:providerId/:id/:tab?";
    }

    @Override
    default Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        List<String> displayFields = getDisplayFields();
        if (!displayFields.isEmpty()) {
            metadata.put("displayFields", displayFields);
        }
        if (supportsDetailTabs()) {
            metadata.put("supportsDetailTabs", true);
            metadata.put("detailTabPath", getDetailTabPath());
        }
        putExtensionMetadata(metadata);
        return metadata;
    }
}
