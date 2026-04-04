import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { environment } from "./environment";
import { i18n } from "./i18n";
import { Root } from "./root/Root";

// Initialize required components before rendering app.
await i18n.init();

const container = document.getElementById("app");
const root = createRoot(container!);

root.render(
  <StrictMode>
    <KeycloakProvider environment={environment}>
      <Root />
    </KeycloakProvider>
  </StrictMode>,
);
