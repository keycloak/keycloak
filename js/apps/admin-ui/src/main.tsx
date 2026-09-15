import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { environment } from "./environment";
import { i18n } from "./i18n/i18n";
import { Root } from "./Root";
import { routes } from "./routes";
import { joinPath } from "./utils/joinPath";

import "./index.css";

// Initialize required components before rendering app.
await i18n.init();

const basename =
  new URL(environment.consoleBaseUrl, window.location.origin).pathname.replace(
    /\/+$/,
    "",
  ) || "/";

if (window.location.hash.startsWith("#/")) {
  const hashPath = decodeURIComponent(window.location.hash.substring(1));
  window.history.replaceState(null, "", joinPath(basename, hashPath));
}

const router = createBrowserRouter(
  [
    {
      path: "/",
      element: <Root />,
      children: routes,
    },
  ],
  { basename },
);
const container = document.getElementById("app");
const root = createRoot(container!);

root.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
