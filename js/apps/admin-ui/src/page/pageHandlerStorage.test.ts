import type { ConfigPropertyRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigInfoRepresentation";
import { describe, expect, it } from "vitest";
import {
  getEntityId,
  interpolateEndpoint,
  normalizeConfig,
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

describe("normalizeConfig", () => {
  it("loads only declared scalar properties for string-map targets", () => {
    const result = normalizeConfig(
      {
        host: "smtp.example.com",
        unrelated: "ignored",
      },
      [scalarProperty],
      "load",
      "string-map",
    );

    expect(result).toEqual({ host: ["smtp.example.com"] });
  });

  it("saves only declared scalar properties for string-map targets", () => {
    const result = normalizeConfig(
      {
        host: ["smtp.example.com"],
        unrelated: ["ignored"],
      },
      [scalarProperty],
      "save",
      "string-map",
    );

    expect(result).toEqual({ host: "smtp.example.com" });
  });

  it("round-trips multivalued string-map values with delimiter", () => {
    const properties = [multivaluedProperty];
    const loaded = normalizeConfig(
      { redirectUris: "https://a.example##https://b.example" },
      properties,
      "load",
      "string-map",
    );

    expect(loaded).toEqual({
      redirectUris: ["https://a.example", "https://b.example"],
    });

    const saved = normalizeConfig(loaded, properties, "save", "string-map");

    expect(saved).toEqual({
      redirectUris: "https://a.example##https://b.example",
    });
  });

  it("keeps multivalued string-map values intact when delimiter is absent", () => {
    const result = normalizeConfig(
      { redirectUris: "https://single.example" },
      [multivaluedProperty],
      "load",
      "string-map",
    );

    expect(result).toEqual({ redirectUris: ["https://single.example"] });
  });

  it("round-trips scalar list-map values", () => {
    const departmentProperty: ConfigPropertyRepresentation = {
      name: "department",
      type: "String",
    };
    const loaded = normalizeConfig(
      { department: "engineering" },
      [departmentProperty],
      "load",
      "list-map",
    );

    expect(loaded).toEqual({ department: ["engineering"] });

    const saved = normalizeConfig(
      loaded,
      [departmentProperty],
      "save",
      "list-map",
    );

    expect(saved).toEqual({ department: ["engineering"] });
  });

  it("round-trips multivalued list-map values", () => {
    const properties = [multivaluedProperty];
    const loaded = normalizeConfig(
      { redirectUris: ["https://a.example", "https://b.example"] },
      properties,
      "load",
      "list-map",
    );

    expect(loaded).toEqual({
      redirectUris: ["https://a.example", "https://b.example"],
    });

    const saved = normalizeConfig(loaded, properties, "save", "list-map");

    expect(saved).toEqual({
      redirectUris: ["https://a.example", "https://b.example"],
    });
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
