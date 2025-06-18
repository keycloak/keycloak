import Resource from "./resource.js";
import type { KeycloakAdminClient } from "../client.js";
import type ClientProfilesRepresentation from "../defs/clientProfilesRepresentation.js";
import type ClientPoliciesRepresentation from "../defs/clientPoliciesRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/15.0/rest-api/#_client_registration_policy_resource
 */
export class ClientPolicies extends Resource<{ realm?: string }> {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/client-policies",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  /* Client Profiles */

  public listProfiles = this.makeRequest<
    { includeGlobalProfiles?: boolean },
    ClientProfilesRepresentation
  >({
    method: "GET",
    path: "/profiles",
    queryParamKeys: ["include-global-profiles"],
    keyTransform: {
      includeGlobalProfiles: "include-global-profiles",
    },
  });

  public createProfiles = this.makeRequest<ClientProfilesRepresentation, void>({
    method: "PUT",
    path: "/profiles",
  });

  /* Client Policies */

  public listPolicies = this.makeRequest<
    { includeGlobalPolicies?: boolean },
    ClientPoliciesRepresentation
  >({
    method: "GET",
    path: "/policies",
    queryParamKeys: ["include-global-policies"],
    keyTransform: {
      includeGlobalPolicies: "include-global-policies",
    },
  });

  public updatePolicy = this.makeRequest<ClientPoliciesRepresentation, void>({
    method: "PUT",
    path: "/policies",
  });
}
