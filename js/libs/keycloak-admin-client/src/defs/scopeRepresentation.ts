/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_scoperepresentation
 */
import type PolicyRepresentation from "./policyRepresentation.js";
import type ResourceRepresentation from "./resourceRepresentation.js";

export default interface ScopeRepresentation {
  displayName?: string;
  iconUri?: string;
  id?: string;
  name?: string;
  policies?: PolicyRepresentation[];
  resources?: ResourceRepresentation[];
}
