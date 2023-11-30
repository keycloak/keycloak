import "@patternfly/react-core/dist/styles/base.css";
import "@patternfly/patternfly/patternfly-addons.css";

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createHashRouter, RouterProvider } from "react-router-dom";

import { i18n } from "./i18n";
import { keycloak } from "./keycloak";
import { routes } from "./routes";

// Initialize required components before rendering app.
await Promise.all([
  keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
  }),
  i18n.init(),
]);

const router = createHashRouter(routes);
const container = document.getElementById("app");
const root = createRoot(container!);

root.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
