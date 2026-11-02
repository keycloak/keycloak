import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchAdminUI, postAdminUI } from "./admin-ui-endpoint";

describe("admin-ui-endpoint", () => {
  let adminClient: KeycloakAdminClient;
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    adminClient = new KeycloakAdminClient({
      baseUrl: "http://localhost:8080",
      realmName: "test",
      requestOptions: { headers: { "X-Custom-Header": "custom" } },
    });
    adminClient.registerTokenProvider({
      getAccessToken: () => Promise.resolve("token"),
    });

    fetchMock = vi.fn().mockResolvedValue(new Response("{}"));
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const sentHeaders = () => new Headers(fetchMock.mock.calls[0][1].headers);

  it("sends the headers configured on the admin client with fetchAdminUI", async () => {
    await fetchAdminUI(adminClient, "ui-ext/info");

    expect(sentHeaders().get("X-Custom-Header")).toBe("custom");
    expect(sentHeaders().get("Authorization")).toBe("Bearer token");
  });

  it("sends the headers configured on the admin client with postAdminUI", async () => {
    await postAdminUI(adminClient, "ui-ext/endpoint", {});

    expect(sentHeaders().get("X-Custom-Header")).toBe("custom");
    expect(sentHeaders().get("Authorization")).toBe("Bearer token");
    expect(sentHeaders().get("Content-Type")).toBe("application/json");
  });
});
