import { KeycloakProvider } from "@keycloak/keycloak-ui-shared-pf6";

import { App } from "./App";
import { environment } from "./environment";

export const Root = () => (
  <KeycloakProvider environment={environment}>
    <App />
  </KeycloakProvider>
);
