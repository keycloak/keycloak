import type ClientPolicyConditionRepresentation from "./clientPolicyConditionRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/15.0/rest-api/#_clientpolicyrepresentation
 */
export default interface ClientPolicyRepresentation {
  conditions?: ClientPolicyConditionRepresentation[];
  description?: string;
  enabled?: boolean;
  mode?: string;
  name?: string;
  profiles?: string[];
}
