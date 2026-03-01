import { KeycloakProvider } from "@keycloak/keycloak-ui-shared";

import { App } from "./App";
import { environment } from "./environment";

export const Root = () => (
  <KeycloakProvider environment={environment}>
    <App />
  </KeycloakProvider>
);
