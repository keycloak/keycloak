import Resource from "./resource.js";
import type OrganizationRepresentation from "../defs/organizationRepresentation.js";

import type { KeycloakAdminClient } from "../client.js";

export interface OrganizationQuery {
  first?: number; // The position of the first result to be processed (pagination offset)
  max?: number; // The maximum number of results to be returned - defaults to 10
  search?: string; // A String representing either an organization name or domain
  q?: string; // A query to search for custom attributes, in the format 'key1:value2 key2:value2'
  exact?: boolean; // Boolean which defines whether the param 'search' must match exactly or not
}

export class Organizations extends Resource<{ realm?: string }> {
  /**
   * Organizations
   */

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/organizations",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  public find = this.makeRequest<
    OrganizationQuery,
    OrganizationRepresentation[]
  >({
    method: "GET",
    path: "/",
  });

  public findOne = this.makeRequest<{ id: string }, OrganizationRepresentation>(
    {
      method: "GET",
      path: "/{id}",
      urlParamKeys: ["id"],
    },
  );

  public create = this.makeRequest<OrganizationRepresentation, { id: string }>({
    method: "POST",
    path: "/",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public delById = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public updateById = this.makeUpdateRequest<
    { id: string },
    OrganizationRepresentation,
    void
  >({
    method: "PUT",
    path: "/{id}",
    urlParamKeys: ["id"],
  });
}
