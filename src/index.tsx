import React, { StrictMode } from "react";
import ReactDOM from "react-dom";

import { App } from "./App";
import { initAdminClient } from "./context/auth/AdminClient";
import { initI18n } from "./i18n";

import "./index.css";

// Hot Module Replacement (HMR) - Remove this snippet to remove HMR.
// Learn more: https://snowpack.dev/concepts/hot-module-replacement
if (import.meta.hot) {
  import.meta.hot.accept();
}

async function initialize() {
  const { keycloak, adminClient } = await initAdminClient();

  await initI18n(adminClient);

  ReactDOM.render(
    <StrictMode>
      <App keycloak={keycloak} adminClient={adminClient} />
    </StrictMode>,
    document.getElementById("app")
  );
}

initialize();
