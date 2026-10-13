import { getInjectedEnvironment } from "@keycloak/keycloak-ui-shared";
import type { Environment } from "./environment-types";

export const environment = getInjectedEnvironment<Environment>();
