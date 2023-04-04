import Resource from "./resource.js";
import type { KeycloakAdminClient } from "../client.js";

export class Cache extends Resource<{ realm?: string }> {
  public clearUserCache = this.makeRequest<{}, void>({
    method: "POST",
    path: "/clear-user-cache",
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
}
