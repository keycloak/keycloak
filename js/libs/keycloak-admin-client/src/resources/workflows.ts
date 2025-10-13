import Resource from "./resource.js";
import type { KeycloakAdminClient } from "../client.js";
import WorkflowRepresentation from "../defs/workflowRepresentation.js";

export class Workflows extends Resource<{ realm?: string }> {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/workflows",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  find = this.makeRequest({
    method: "GET",
    path: "/",
  });

  public create = this.makeRequest<WorkflowRepresentation, { id: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public delById = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });
}
