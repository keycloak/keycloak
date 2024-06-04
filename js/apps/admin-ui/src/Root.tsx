import {
  KeycloakProvider,
  environmentAdmin as environment,
} from "@keycloak/keycloak-ui-shared";
import { App } from "./App";

export const Root = () => (
  <KeycloakProvider environment={environment}>
    <App />
  </KeycloakProvider>
);
