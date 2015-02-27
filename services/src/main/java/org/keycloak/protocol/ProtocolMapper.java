package org.keycloak.protocol;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ProtocolMapper extends Provider, ProviderFactory<ProtocolMapper> {
    String getProtocol();
    String getDisplayCategory();
    String getDisplayType();
    String getHelpText();

    public static class ConfigProperty {
        protected String name;
        protected String label;
        protected String helpText;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getHelpText() {
            return helpText;
        }

        public void setHelpText(String helpText) {
            this.helpText = helpText;
        }
    }

    List<ConfigProperty> getConfigProperties();
}
