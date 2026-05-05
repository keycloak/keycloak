import { getInjectedEnvironment } from "@keycloak/keycloak-ui-shared";
import type { Environment } from "./environment.d";

export const environment = getInjectedEnvironment<Environment>();
