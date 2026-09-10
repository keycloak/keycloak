import { getInjectedEnvironment } from "@keycloak/keycloak-ui-shared";
import { type AccountEnvironment } from ".";

export const environment = getInjectedEnvironment<AccountEnvironment>();
