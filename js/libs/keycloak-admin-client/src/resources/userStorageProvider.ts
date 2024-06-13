import type { KeycloakAdminClient } from "../client.js";
import type SynchronizationResultRepresentation from "../defs/synchronizationResultRepresentation.js";
import Resource from "./resource.js";

type ActionType = "triggerFullSync" | "triggerChangedUsersSync";
export type DirectionType = "fedToKeycloak" | "keycloakToFed";
type NameResponse = {
  id: string;
  name: string;
};

export class UserStorageProvider extends Resource<{ realm?: string }> {
  public name = this.makeRequest<{ id: string }, NameResponse>({
    method: "GET",
    path: "/{id}/name",
    urlParamKeys: ["id"],
  });

  public removeImportedUsers = this.makeRequest<{ id: string }, void>({
    method: "POST",
    path: "/{id}/remove-imported-users",
    urlParamKeys: ["id"],
  });

  public sync = this.makeRequest<
    { id: string; action?: ActionType },
    SynchronizationResultRepresentation
  >({
    method: "POST",
    path: "/{id}/sync",
    urlParamKeys: ["id"],
    queryParamKeys: ["action"],
  });

  public unlinkUsers = this.makeRequest<{ id: string }, void>({
    method: "POST",
    path: "/{id}/unlink-users",
    urlParamKeys: ["id"],
  });

  public mappersSync = this.makeRequest<
    { id: string; parentId: string; direction?: DirectionType },
    SynchronizationResultRepresentation
  >({
    method: "POST",
    path: "/{parentId}/mappers/{id}/sync",
    urlParamKeys: ["id", "parentId"],
    queryParamKeys: ["direction"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/user-storage",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
