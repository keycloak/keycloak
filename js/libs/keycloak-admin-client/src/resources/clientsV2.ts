import type { KeycloakAdminClient } from "../client.js";
import { Configuration, ClientsV2Api } from "../generated/index.js";

// Re-export types for convenience
export { ClientsV2Api } from "../generated/index.js";
export type {
  AdminApiRealmNameClientsVersionGetRequest,
  AdminApiRealmNameClientsVersionIdDeleteRequest,
  AdminApiRealmNameClientsVersionIdGetRequest,
  AdminApiRealmNameClientsVersionIdPatchRequest,
  AdminApiRealmNameClientsVersionIdPutRequest,
  AdminApiRealmNameClientsVersionPostRequest,
} from "../generated/apis/ClientsV2Api.js";
export type {
  OIDCClientRepresentation,
  SAMLClientRepresentation,
  AdminApiRealmNameClientsVersionGet200ResponseInner as ClientRepresentationV2,
} from "../generated/models/index.js";

/**
 * Creates a ClientsV2Api instance configured with the KeycloakAdminClient's
 * base URL and access token.
 */
export async function createClientsV2Api(
  client: KeycloakAdminClient,
): Promise<ClientsV2Api> {
  const accessToken = await client.getAccessToken();

  const config = new Configuration({
    basePath: client.baseUrl,
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
  });

  return new ClientsV2Api(config);
}

/**
 * Clients v2 API resource.
 * Provides access to the OpenAPI-generated ClientsV2Api.
 */
export class ClientsV2 {
  #client: KeycloakAdminClient;

  constructor(client: KeycloakAdminClient) {
    this.#client = client;
  }

  /**
   * Get the ClientsV2Api instance configured with the current access token.
   * Call this method to get an API instance, then use its methods directly.
   *
   * @example
   * ```typescript
   * const api = await client.clients.v2.api();
   * const clients = await api.adminApiRealmNameClientsVersionGet({
   *   realmName: "master",
   *   version: "v2",
   * });
   * ```
   */
  async api(): Promise<ClientsV2Api> {
    return createClientsV2Api(this.#client);
  }
}
