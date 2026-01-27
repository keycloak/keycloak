import {
  type AuthenticationProvider,
  type RequestInformation,
} from "@microsoft/kiota-abstractions";
import { FetchRequestAdapter } from "@microsoft/kiota-http-fetchlibrary";

import type { KeycloakAdminClient } from "../client.js";
import {
  createAdminClient,
  type AdminClient,
} from "../generated/adminClient.js";

// Re-export types for convenience
export type {
  OIDCClientRepresentation,
  SAMLClientRepresentation,
} from "../generated/models/index.js";

export type ClientRepresentationV2 =
  | import("../generated/models/index.js").OIDCClientRepresentation
  | import("../generated/models/index.js").SAMLClientRepresentation;

/**
 * Authentication provider for Keycloak Admin Client that adds Bearer token to requests.
 */
class KeycloakAuthProvider implements AuthenticationProvider {
  #getAccessToken: () => Promise<string | undefined>;

  constructor(getAccessToken: () => Promise<string | undefined>) {
    this.#getAccessToken = getAccessToken;
  }

  async authenticateRequest(request: RequestInformation): Promise<void> {
    const token = await this.#getAccessToken();
    if (token) {
      request.headers.add("Authorization", `Bearer ${token}`);
    }
  }
}

/**
 * Creates a Kiota AdminClient instance configured with the KeycloakAdminClient's
 * base URL and access token.
 */
export async function createKiotaAdminClient(
  client: KeycloakAdminClient,
): Promise<AdminClient> {
  const authProvider = new KeycloakAuthProvider(() => client.getAccessToken());
  const adapter = new FetchRequestAdapter(authProvider);
  adapter.baseUrl = client.baseUrl;

  return createAdminClient(adapter);
}

/**
 * Clients v2 API resource.
 * Provides access to the Kiota-generated AdminClient with fluent API.
 *
 * @example
 * ```typescript
 * const api = await client.clients.v2.api();
 * const clients = await api.admin.api.byRealmName("master").clients.byVersion("v2").get();
 * ```
 */
export class ClientsV2 {
  #client: KeycloakAdminClient;

  constructor(client: KeycloakAdminClient) {
    this.#client = client;
  }

  /**
   * Get the AdminClient instance configured with the current access token.
   * Call this method to get an API instance, then use its fluent methods.
   *
   * @example
   * ```typescript
   * const api = await client.clients.v2.api();
   *
   * // List all clients
   * const clients = await api.admin.api.byRealmName("master").clients.byVersion("v2").get();
   *
   * // Get a single client
   * const client = await api.admin.api.byRealmName("master").clients.byVersion("v2").byId("my-client").get();
   *
   * // Create a client
   * await api.admin.api.byRealmName("master").clients.byVersion("v2").post({
   *   clientId: "my-client",
   *   protocol: "openid-connect",
   *   enabled: true,
   * });
   *
   * // Update a client
   * await api.admin.api.byRealmName("master").clients.byVersion("v2").byId("my-client").put({
   *   clientId: "my-client",
   *   protocol: "openid-connect",
   *   description: "Updated description",
   * });
   *
   * // Delete a client
   * await api.admin.api.byRealmName("master").clients.byVersion("v2").byId("my-client").delete();
   * ```
   */
  async api(): Promise<AdminClient> {
    return createKiotaAdminClient(this.#client);
  }
}
