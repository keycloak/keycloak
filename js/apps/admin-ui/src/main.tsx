import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createHashRouter, RouterProvider } from "react-router-dom";
import { i18n } from "./i18n/i18n";
import { Root } from "./Root";
import { routes } from "./routes";

import "./index.css";

// Initialize required components before rendering app.
await i18n.init();

const router = createHashRouter([
  {
    path: "/",
    element: <Root />,
    children: routes,
  },
]);
const container = document.getElementById("app");
const root = createRoot(container!);

root.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
