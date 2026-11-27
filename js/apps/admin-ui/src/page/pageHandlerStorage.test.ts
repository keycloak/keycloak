import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { describe, expect, it } from "vitest";
import {
  getEntityId,
  interpolateEndpoint,
  isStringMapStorageType,
  mergeEntityConfig,
  pickDeclaredConfig,
  resolveTabParams,
} from "./pageHandlerStorage";

const scalarProperty: ConfigPropertyRepresentation = {
  name: "host",
  type: "String",
};

const multivaluedProperty: ConfigPropertyRepresentation = {
  name: "redirectUris",
  type: "MultivaluedString",
};

describe("resolveTabParams", () => {
  it("merges route params with params from the metadata path", () => {
    const params = resolveTabParams(
      "/master/clients/abc-123/settings",
      "/:realm/clients/:clientId/:tab?",
      { realm: "master" },
    );

    expect(params).toEqual({
      realm: "master",
      clientId: "abc-123",
      tab: "settings",
    });
  });

  it("returns route params when metadata path does not match", () => {
    const params = resolveTabParams(
      "/master/users/user-1",
      "/:realm/clients/:clientId/:tab?",
      { realm: "master", id: "user-1" },
    );

    expect(params).toEqual({ realm: "master", id: "user-1" });
  });
});

describe("getEntityId", () => {
  it("resolves entity ids from route params", () => {
    expect(
      getEntityId("CLIENT", { clientId: "client-1", realm: "master" }),
    ).toBe("client-1");
    expect(getEntityId("USER", { id: "user-1", realm: "master" })).toBe(
      "user-1",
    );
    expect(
      getEntityId("IDENTITY_PROVIDER", {
        alias: "google",
        providerId: "oidc",
      }),
    ).toBe("google");
    expect(getEntityId("COMPONENT", { clientId: "client-1" })).toBeUndefined();
  });
});

describe("isStringMapStorageType", () => {
  it("identifies string-map storage types", () => {
    expect(isStringMapStorageType("CLIENT")).toBe(true);
    expect(isStringMapStorageType("IDENTITY_PROVIDER")).toBe(true);
    expect(isStringMapStorageType("USER")).toBe(false);
    expect(isStringMapStorageType("COMPONENT")).toBe(false);
  });
});

describe("pickDeclaredConfig", () => {
  it("returns only declared properties", () => {
    const result = pickDeclaredConfig(
      {
        host: "smtp.example.com",
        unrelated: "ignored",
      },
      [scalarProperty],
    );

    expect(result).toEqual({ host: "smtp.example.com" });
  });

  it("omits undefined and null values", () => {
    const result = pickDeclaredConfig(
      {
        host: undefined,
        redirectUris: null,
      },
      [scalarProperty, multivaluedProperty],
    );

    expect(result).toEqual({});
  });
});

describe("mergeEntityConfig", () => {
  it("removes cleared declared properties from existing values", () => {
    const result = mergeEntityConfig(
      { host: "old.example.com", keep: "value" },
      { host: undefined },
      [scalarProperty],
      "string-map",
    );

    expect(result).toEqual({ keep: "value" });
  });

  it("updates declared scalar properties for string-map targets", () => {
    const result = mergeEntityConfig(
      { host: "old.example.com", keep: "value" },
      { host: "new.example.com" },
      [scalarProperty],
      "string-map",
    );

    expect(result).toEqual({
      host: "new.example.com",
      keep: "value",
    });
  });

  it("joins multivalued string-map values with the delimiter", () => {
    const result = mergeEntityConfig(
      {},
      { redirectUris: "https://a.example##https://b.example" },
      [multivaluedProperty],
      "string-map",
    );

    expect(result).toEqual({
      redirectUris: "https://a.example##https://b.example",
    });
  });

  it("wraps scalar list-map values in arrays", () => {
    const departmentProperty: ConfigPropertyRepresentation = {
      name: "department",
      type: "String",
    };
    const result = mergeEntityConfig(
      {},
      { department: "engineering" },
      [departmentProperty],
      "list-map",
    );

    expect(result).toEqual({ department: ["engineering"] });
  });

  it("keeps multivalued list-map values as arrays", () => {
    const result = mergeEntityConfig(
      {},
      { redirectUris: ["https://a.example", "https://b.example"] },
      [multivaluedProperty],
      "list-map",
    );

    expect(result).toEqual({
      redirectUris: ["https://a.example", "https://b.example"],
    });
  });

  it("removes cleared multiline values represented as an empty string array", () => {
    const multilineProperty: ConfigPropertyRepresentation = {
      name: "notes",
      type: "MultivaluedString",
    };
    const result = mergeEntityConfig(
      { notes: "existing" },
      { notes: [""] },
      [multilineProperty],
      "string-map",
    );

    expect(result).toEqual({});
  });
});

describe("interpolateEndpoint", () => {
  it("replaces placeholders with encoded values", () => {
    expect(
      interpolateEndpoint("extensions/clients/{clientId}/settings", {
        clientId: "abc/123",
      }),
    ).toBe("extensions/clients/abc%2F123/settings");
  });

  it("throws when a placeholder is missing", () => {
    expect(() =>
      interpolateEndpoint("extensions/clients/{clientId}/settings", {}),
    ).toThrow("Missing endpoint parameter(s): clientId");
  });
});
