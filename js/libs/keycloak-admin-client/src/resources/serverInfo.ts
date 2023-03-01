import Resource from "./resource.js";
import type { ServerInfoRepresentation } from "../defs/serverInfoRepesentation.js";
import type KeycloakAdminClient from "../index.js";

export class ServerInfo extends Resource {
  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/serverinfo",
      getBaseUrl: () => client.baseUrl,
    });
  }

  public find = this.makeRequest<{}, ServerInfoRepresentation>({
    method: "GET",
    path: "/",
  });
}
