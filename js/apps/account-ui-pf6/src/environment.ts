import { getInjectedEnvironment } from "@keycloak/keycloak-ui-shared-pf6";
import { type AccountEnvironment } from ".";

export const environment = getInjectedEnvironment<AccountEnvironment>();
