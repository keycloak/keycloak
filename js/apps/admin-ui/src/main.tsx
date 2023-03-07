import "@patternfly/patternfly/patternfly-addons.css";
import "@patternfly/react-core/dist/styles/base.css";

import { StrictMode } from "react";
import { render } from "react-dom";

import { App } from "./App";
import { initAdminClient } from "./context/auth/AdminClient";
import { initI18n } from "./i18n";

import "./index.css";

const { keycloak, adminClient } = await initAdminClient();

await initI18n(adminClient);

const container = document.getElementById("app");

render(
  <StrictMode>
    <App keycloak={keycloak} adminClient={adminClient} />
  </StrictMode>,
  container
);
