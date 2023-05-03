import "@patternfly/react-core/dist/styles/base.css";
import "@patternfly/patternfly/patternfly-addons.css";

import { StrictMode } from "react";
import { render } from "react-dom";
import { createHashRouter, RouterProvider } from "react-router-dom";

import { initI18n } from "./i18n";
import { keycloak } from "./keycloak";
import { RootRoute } from "./routes";

import "./index.css";

// Initialize required components before rendering app.
await keycloak.init({ onLoad: "check-sso", pkceMethod: "S256" });
await initI18n();

const router = createHashRouter([RootRoute]);
const container = document.getElementById("app");

render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
  container
);
