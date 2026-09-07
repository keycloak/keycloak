package org.keycloak.services.ui.extend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.models.AdminRoles;

import static org.junit.Assert.assertEquals;

public class UiExtensionSupportTest {

    private final UiExtensionSupport support = new UiExtensionSupport() {
    };

    @Test
    public void defaultRolesAndNavSection() {
        assertEquals(List.of(AdminRoles.VIEW_REALM), support.getRequiredViewRoles());
        assertEquals(List.of(AdminRoles.MANAGE_REALM), support.getRequiredManageRoles());
        assertEquals("configure", support.getNavSection());
    }

    @Test
    public void putExtensionMetadataIncludesDeclaredValues() {
        UiExtensionSupport customSupport = new UiExtensionSupport() {
            @Override
            public List<String> getRequiredViewRoles() {
                return List.of(AdminRoles.VIEW_CLIENTS);
            }

            @Override
            public List<String> getRequiredManageRoles() {
                return List.of(AdminRoles.MANAGE_CLIENTS);
            }

            @Override
            public String getNavSection() {
                return "extensions";
            }

            @Override
            public String getTabLabel() {
                return "customTab";
            }
        };

        Map<String, Object> metadata = new HashMap<>();
        customSupport.putExtensionMetadata(metadata);

        assertEquals(List.of(AdminRoles.VIEW_CLIENTS), metadata.get("requiredViewRoles"));
        assertEquals(List.of(AdminRoles.MANAGE_CLIENTS), metadata.get("requiredManageRoles"));
        assertEquals("extensions", metadata.get("navSection"));
        assertEquals("customTab", metadata.get("tabLabel"));
    }
}
