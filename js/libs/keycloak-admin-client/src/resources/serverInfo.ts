import Resource from "./resource.js";
import type { ServerInfoRepresentation } from "../defs/serverInfoRepesentation.js";
import type KeycloakAdminClient from "../index.js";
import EffectiveMessageBundleRepresentation from "../defs/effectiveMessageBundleRepresentation.js";

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

  public findEffectiveMessageBundles = this.makeRequest<
    {
      realm: string;
      theme?: string;
      themeType?: string;
      local?: string;
      hasWords?: string;
      source?: boolean;
      first?: number;
      max?: number;
    },
    EffectiveMessageBundleRepresentation[]
  >({
    method: "GET",
    path: "/resources/{realm}/{themeType}/{locale}",
    urlParamKeys: ["realm", "themeType", "locale"],
    queryParamKeys: ["theme", "source"],
  });
}
