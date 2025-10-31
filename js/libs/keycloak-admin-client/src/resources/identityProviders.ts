import type { KeycloakAdminClient } from "../client.js";
import type IdentityProviderMapperRepresentation from "../defs/identityProviderMapperRepresentation.js";
import type { IdentityProviderMapperTypeRepresentation } from "../defs/identityProviderMapperTypeRepresentation.js";
import type IdentityProviderRepresentation from "../defs/identityProviderRepresentation.js";
import type { ManagementPermissionReference } from "../defs/managementPermissionReference.js";
import Resource from "./resource.js";

export interface PaginatedQuery {
  first?: number;
  max?: number;
}

export interface IdentityProvidersQuery extends PaginatedQuery {
  search?: string;
  realmOnly?: boolean;
  type?: string;
  capability?: string;
}

export class IdentityProviders extends Resource<{ realm?: string }> {
  /**
   * Identity provider
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_identity_providers_resource
   */

  public find = this.makeRequest<
    IdentityProvidersQuery,
    IdentityProviderRepresentation[]
  >({
    method: "GET",
    path: "/instances",
  });

  public create = this.makeRequest<
    IdentityProviderRepresentation,
    { id: string }
  >({
    method: "POST",
    path: "/instances",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public findOne = this.makeRequest<
    { alias: string },
    IdentityProviderRepresentation | undefined
  >({
    method: "GET",
    path: "/instances/{alias}",
    urlParamKeys: ["alias"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { alias: string },
    IdentityProviderRepresentation,
    void
  >({
    method: "PUT",
    path: "/instances/{alias}",
    urlParamKeys: ["alias"],
  });

  public del = this.makeRequest<{ alias: string }, void>({
    method: "DELETE",
    path: "/instances/{alias}",
    urlParamKeys: ["alias"],
  });

  public findFactory = this.makeRequest<{ providerId: string }, any>({
    method: "GET",
    path: "/providers/{providerId}",
    urlParamKeys: ["providerId"],
  });

  public findMappers = this.makeRequest<
    { alias: string },
    IdentityProviderMapperRepresentation[]
  >({
    method: "GET",
    path: "/instances/{alias}/mappers",
    urlParamKeys: ["alias"],
  });

  public findOneMapper = this.makeRequest<
    { alias: string; id: string },
    IdentityProviderMapperRepresentation | undefined
  >({
    method: "GET",
    path: "/instances/{alias}/mappers/{id}",
    urlParamKeys: ["alias", "id"],
    catchNotFound: true,
  });

  public createMapper = this.makeRequest<
    {
      alias: string;
      identityProviderMapper: IdentityProviderMapperRepresentation;
    },
    { id: string }
  >({
    method: "POST",
    path: "/instances/{alias}/mappers",
    urlParamKeys: ["alias"],
    payloadKey: "identityProviderMapper",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateMapper = this.makeUpdateRequest<
    { alias: string; id: string },
    IdentityProviderMapperRepresentation,
    void
  >({
    method: "PUT",
    path: "/instances/{alias}/mappers/{id}",
    urlParamKeys: ["alias", "id"],
  });

  public delMapper = this.makeRequest<{ alias: string; id: string }, void>({
    method: "DELETE",
    path: "/instances/{alias}/mappers/{id}",
    urlParamKeys: ["alias", "id"],
  });

  public findMapperTypes = this.makeRequest<
    { alias: string },
    Record<string, IdentityProviderMapperTypeRepresentation>
  >({
    method: "GET",
    path: "/instances/{alias}/mapper-types",
    urlParamKeys: ["alias"],
  });

  public importFromUrl = this.makeRequest<
    | {
        fromUrl: string;
        providerId: string;
      }
    | FormData,
    Record<string, string>
  >({
    method: "POST",
    path: "/import-config",
  });

  public updatePermission = this.makeUpdateRequest<
    { alias: string },
    ManagementPermissionReference,
    ManagementPermissionReference
  >({
    method: "PUT",
    path: "/instances/{alias}/management/permissions",
    urlParamKeys: ["alias"],
  });

  public listPermissions = this.makeRequest<
    { alias: string },
    ManagementPermissionReference
  >({
    method: "GET",
    path: "/instances/{alias}/management/permissions",
    urlParamKeys: ["alias"],
  });

  public reloadKeys = this.makeRequest<{ alias: string }, boolean>({
    method: "GET",
    path: "/instances/{alias}/reload-keys",
    urlParamKeys: ["alias"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/identity-provider",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
