import AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

import type { UsedByClientRef } from "../components/role-mapping/resource";

export type { UsedByClientRef };

type UsedBy = "SPECIFIC_CLIENTS" | "SPECIFIC_PROVIDERS" | "DEFAULT";

export type AuthenticationType = AuthenticationFlowRepresentation & {
  usedBy?: {
    type?: UsedBy;
    values: string[];
    clientRefs?: UsedByClientRef[];
  };
  realm: RealmRepresentation;
};

export const REALM_FLOWS = new Map<string, string>([
  ["browserFlow", "browser"],
  ["registrationFlow", "registration"],
  ["directGrantFlow", "direct grant"],
  ["resetCredentialsFlow", "reset credentials"],
  ["clientAuthenticationFlow", "clients"],
  ["dockerAuthenticationFlow", "docker auth"],
  ["firstBrokerLoginFlow", "firstBrokerLogin"],
]);
