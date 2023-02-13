import Resource from "./resource.js";
import type KeycloakAdminClient from "../index.js";

export class Sessions extends Resource<{ realm?: string }> {
  public find = this.makeRequest<{}, Record<string, any>[]>({
    method: "GET",
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/client-session-stats",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
