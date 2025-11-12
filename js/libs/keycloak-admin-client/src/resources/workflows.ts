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

  public findOne = this.makeRequest<
    { id: string; includeId: boolean },
    WorkflowRepresentation | undefined
  >({
    method: "GET",
    path: "/{id}",
    urlParamKeys: ["id"],
    queryParamKeys: ["includeId"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { id: string },
    WorkflowRepresentation,
    void
  >({
    method: "PUT",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public create = this.makeRequest<WorkflowRepresentation, { id: string }>({
    method: "POST",
    headers: { "Content-Type": "application/json" },
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public createAsYaml = this.makeRequest<
    { realm: string; yaml: string },
    { id: string }
  >({
    method: "POST",
    headers: { "Content-Type": "application/yaml", Accept: "application/yaml" },
    returnResourceIdInLocationHeader: { field: "id" },
    payloadKey: "yaml",
  });

  public delById = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });
}
