import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { environment } from "./environment";
import { i18n } from "./i18n";
import { Root } from "./root/Root";

const DARK_MODE_CLASS = "pf-v6-theme-dark";

const syncDarkMode = (isEnabled: boolean) => {
  document.documentElement.classList.toggle(DARK_MODE_CLASS, isEnabled);
};

if (environment.darkMode) {
  const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
  syncDarkMode(mediaQuery.matches);
  mediaQuery.addEventListener("change", (event) => syncDarkMode(event.matches));
} else {
  syncDarkMode(false);
}

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
