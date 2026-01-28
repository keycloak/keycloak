import type ClientScopeRepresentation from "../defs/clientScopeRepresentation.js";
import Resource from "./resource.js";
import type { KeycloakAdminClient } from "../client.js";
import type ProtocolMapperRepresentation from "../defs/protocolMapperRepresentation.js";
import type MappingsRepresentation from "../defs/mappingsRepresentation.js";
import type RoleRepresentation from "../defs/roleRepresentation.js";

export class ClientScopes extends Resource<{ realm?: string }> {
  public find = this.makeRequest<{}, ClientScopeRepresentation[]>({
    method: "GET",
    path: "/client-scopes",
  });

  public create = this.makeRequest<ClientScopeRepresentation, { id: string }>({
    method: "POST",
    path: "/client-scopes",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  /**
   * Client-Scopes by id
   */

  public findOne = this.makeRequest<
    { id: string },
    ClientScopeRepresentation | undefined
  >({
    method: "GET",
    path: "/client-scopes/{id}",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { id: string },
    ClientScopeRepresentation,
    void
  >({
    method: "PUT",
    path: "/client-scopes/{id}",
    urlParamKeys: ["id"],
  });

  public del = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/client-scopes/{id}",
    urlParamKeys: ["id"],
  });

  /**
   * Default Client-Scopes
   */

  public listDefaultClientScopes = this.makeRequest<
    void,
    ClientScopeRepresentation[]
  >({
    method: "GET",
    path: "/default-default-client-scopes",
  });

  public addDefaultClientScope = this.makeRequest<{ id: string }, void>({
    method: "PUT",
    path: "/default-default-client-scopes/{id}",
    urlParamKeys: ["id"],
  });

  public delDefaultClientScope = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/default-default-client-scopes/{id}",
    urlParamKeys: ["id"],
  });

  /**
   * Default Optional Client-Scopes
   */

  public listDefaultOptionalClientScopes = this.makeRequest<
    void,
    ClientScopeRepresentation[]
  >({
    method: "GET",
    path: "/default-optional-client-scopes",
  });

  public addDefaultOptionalClientScope = this.makeRequest<{ id: string }, void>(
    {
      method: "PUT",
      path: "/default-optional-client-scopes/{id}",
      urlParamKeys: ["id"],
    },
  );

  public delDefaultOptionalClientScope = this.makeRequest<{ id: string }, void>(
    {
      method: "DELETE",
      path: "/default-optional-client-scopes/{id}",
      urlParamKeys: ["id"],
    },
  );

  /**
   * Protocol Mappers
   */

  public addMultipleProtocolMappers = this.makeUpdateRequest<
    { id: string },
    ProtocolMapperRepresentation[],
    void
  >({
    method: "POST",
    path: "/client-scopes/{id}/protocol-mappers/add-models",
    urlParamKeys: ["id"],
  });

  public addProtocolMapper = this.makeUpdateRequest<
    { id: string },
    ProtocolMapperRepresentation,
    void
  >({
    method: "POST",
    path: "/client-scopes/{id}/protocol-mappers/models",
    urlParamKeys: ["id"],
  });

  public listProtocolMappers = this.makeRequest<
    { id: string },
    ProtocolMapperRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/protocol-mappers/models",
    urlParamKeys: ["id"],
  });

  public findProtocolMapper = this.makeRequest<
    { id: string; mapperId: string },
    ProtocolMapperRepresentation | undefined
  >({
    method: "GET",
    path: "/client-scopes/{id}/protocol-mappers/models/{mapperId}",
    urlParamKeys: ["id", "mapperId"],
    catchNotFound: true,
  });

  public findProtocolMappersByProtocol = this.makeRequest<
    { id: string; protocol: string },
    ProtocolMapperRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/protocol-mappers/protocol/{protocol}",
    urlParamKeys: ["id", "protocol"],
    catchNotFound: true,
  });

  public updateProtocolMapper = this.makeUpdateRequest<
    { id: string; mapperId: string },
    ProtocolMapperRepresentation,
    void
  >({
    method: "PUT",
    path: "/client-scopes/{id}/protocol-mappers/models/{mapperId}",
    urlParamKeys: ["id", "mapperId"],
  });

  public delProtocolMapper = this.makeRequest<
    { id: string; mapperId: string },
    void
  >({
    method: "DELETE",
    path: "/client-scopes/{id}/protocol-mappers/models/{mapperId}",
    urlParamKeys: ["id", "mapperId"],
  });

  /**
   * Scope Mappings
   */
  public listScopeMappings = this.makeRequest<
    { id: string },
    MappingsRepresentation
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings",
    urlParamKeys: ["id"],
  });

  public addClientScopeMappings = this.makeUpdateRequest<
    { id: string; client: string },
    RoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/client-scopes/{id}/scope-mappings/clients/{client}",
    urlParamKeys: ["id", "client"],
  });

  public listClientScopeMappings = this.makeRequest<
    { id: string; client: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/clients/{client}",
    urlParamKeys: ["id", "client"],
  });

  public listAvailableClientScopeMappings = this.makeRequest<
    { id: string; client: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/clients/{client}/available",
    urlParamKeys: ["id", "client"],
  });

  public listCompositeClientScopeMappings = this.makeRequest<
    { id: string; client: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/clients/{client}/composite",
    urlParamKeys: ["id", "client"],
  });

  public delClientScopeMappings = this.makeUpdateRequest<
    { id: string; client: string },
    RoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/client-scopes/{id}/scope-mappings/clients/{client}",
    urlParamKeys: ["id", "client"],
  });

  public addRealmScopeMappings = this.makeUpdateRequest<
    { id: string },
    RoleRepresentation[],
    void
  >({
    method: "POST",
    path: "/client-scopes/{id}/scope-mappings/realm",
    urlParamKeys: ["id"],
  });

  public listRealmScopeMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/realm",
    urlParamKeys: ["id"],
  });

  public listAvailableRealmScopeMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/realm/available",
    urlParamKeys: ["id"],
  });

  public listCompositeRealmScopeMappings = this.makeRequest<
    { id: string },
    RoleRepresentation[]
  >({
    method: "GET",
    path: "/client-scopes/{id}/scope-mappings/realm/composite",
    urlParamKeys: ["id"],
  });

  public delRealmScopeMappings = this.makeUpdateRequest<
    { id: string },
    RoleRepresentation[],
    void
  >({
    method: "DELETE",
    path: "/client-scopes/{id}/scope-mappings/realm",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  /**
   * Find client scope by name.
   */
  public async findOneByName(payload: {
    realm?: string;
    name: string;
  }): Promise<ClientScopeRepresentation | undefined> {
    const allScopes = await this.find({
      ...(payload.realm ? { realm: payload.realm } : {}),
    });
    return allScopes.find((item) => item.name === payload.name);
  }

  /**
   * Delete client scope by name.
   */
  public async delByName(payload: {
    realm?: string;
    name: string;
  }): Promise<void> {
    const scope = await this.findOneByName(payload);

    if (!scope) {
      throw new Error("Scope not found.");
    }

    await this.del({
      ...(payload.realm ? { realm: payload.realm } : {}),
      id: scope.id!,
    });
  }

  /**
   * Find single protocol mapper by name.
   */
  public async findProtocolMapperByName(payload: {
    realm?: string;
    id: string;
    name: string;
  }): Promise<ProtocolMapperRepresentation | undefined> {
    const allProtocolMappers = await this.listProtocolMappers({
      id: payload.id,
      ...(payload.realm ? { realm: payload.realm } : {}),
    });
    return allProtocolMappers.find((mapper) => mapper.name === payload.name);
  }
}
