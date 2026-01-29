import {
  type AuthenticationProvider,
  type RequestInformation,
} from "@microsoft/kiota-abstractions";
import { FetchRequestAdapter } from "@microsoft/kiota-http-fetchlibrary";

import type { KeycloakAdminClient } from "../client.js";
import { createAdminClient } from "../generated/adminClient.js";
import type { WithVersionItemRequestBuilder } from "../generated/admin/api/item/clients/item/index.js";

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
 * Creates a clients v2 endpoint configured with the KeycloakAdminClient's
 * base URL, access token, and realm.
 */
function createClientsV2Endpoint(
  client: KeycloakAdminClient,
): WithVersionItemRequestBuilder {
  const authProvider = new KeycloakAuthProvider(() => client.getAccessToken());
  const adapter = new FetchRequestAdapter(authProvider);
  adapter.baseUrl = client.baseUrl;

  const adminClient = createAdminClient(adapter);
  return adminClient.admin.api
    .byRealmName(client.realmName)
    .clients.byVersion("v2");
}

/**
 * Clients v2 API resource.
 * Provides access to the new versioned clients API using the configured realm.
 */
export class ClientsV2 {
  #client: KeycloakAdminClient;

  constructor(client: KeycloakAdminClient) {
    this.#client = client;
  }

  /**
   * Get the clients v2 endpoint for the currently configured realm.
   * Returns a fluent API builder for client operations.
   *
   * @returns The clients v2 endpoint
   */
  api(): WithVersionItemRequestBuilder {
    return createClientsV2Endpoint(this.#client);
  }
}
