import "@patternfly/react-core/dist/styles/base.css";
import "@patternfly/patternfly/patternfly-addons.css";

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";

import { environment } from "./environment";
import { i18n } from "./i18n";
import { routes } from "./routes";
import { getRootPath } from "./utils/getRootPath";

// Initialize required components before rendering app.
await i18n.init();

const container = document.getElementById("app");
const root = createRoot(container!);

const basename = getRootPath(environment.realm);
const router = createBrowserRouter(routes, { basename });

root.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
