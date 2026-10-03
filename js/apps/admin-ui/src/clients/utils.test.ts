import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { describe, expect, it } from "vitest";
import { omitClientScopes } from "./utils";

describe("omitClientScopes", () => {
  it("removes default and optional client scopes and keeps other fields", () => {
    const client: ClientRepresentation = {
      id: "123",
      clientId: "my-client",
      redirectUris: ["http://example.org/auth"],
      defaultClientScopes: ["profile", "roles"],
      optionalClientScopes: ["scope-1", "scope-2"],
    };

    const result = omitClientScopes(client);

    expect(result).toEqual({
      id: "123",
      clientId: "my-client",
      redirectUris: ["http://example.org/auth"],
    });
    expect(client.defaultClientScopes).toEqual(["profile", "roles"]);
    expect(client.optionalClientScopes).toEqual(["scope-1", "scope-2"]);
  });
});
