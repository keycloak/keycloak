import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createHashRouter, RouterProvider } from "react-router-dom";
import { i18n } from "./i18n/i18n";
import { RootRoute } from "./routes";

import "./index.css";

// Initialize required components before rendering app.
await i18n.init();

// When returning from an OAuth redirect, the URL has query params (code, state)
// but the hash fragment is lost. Restore it from sessionStorage so the router
// matches the correct route on creation.
const searchParams = new URLSearchParams(window.location.search);
if (searchParams.has("code") || searchParams.has("state")) {
  const savedHash = sessionStorage.getItem("kc-hash-before-login");
  if (savedHash) {
    sessionStorage.removeItem("kc-hash-before-login");
    window.location.hash = savedHash;
  }
} else if (window.location.hash && window.location.hash !== "#" && window.location.hash !== "#/") {
  sessionStorage.setItem("kc-hash-before-login", window.location.hash);
}

const router = createHashRouter([RootRoute]);
const container = document.getElementById("app");
const root = createRoot(container!);

root.render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
