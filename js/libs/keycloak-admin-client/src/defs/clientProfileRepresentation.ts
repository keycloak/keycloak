import type ClientPolicyExecutorRepresentation from "./clientPolicyExecutorRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/15.0/rest-api/#_clientprofilerepresentation
 */
export default interface ClientProfileRepresentation {
  description?: string;
  executors?: ClientPolicyExecutorRepresentation[];
  name?: string;
}
