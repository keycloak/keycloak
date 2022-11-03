import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import ReactDOM from "react-dom";
import { createBrowserRouter, RouterProvider } from "react-router-dom";

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

const router = createBrowserRouter(routes);

ReactDOM.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
  document.getElementById("app")
);
