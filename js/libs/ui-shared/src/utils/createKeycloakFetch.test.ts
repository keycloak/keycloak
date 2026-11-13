import { describe, expect, it, vi } from "vitest";
import type Keycloak from "keycloak-js";
import { createKeycloakFetch } from "./createKeycloakFetch";

describe("createKeycloakFetch", () => {
  it("resyncs a stale Bearer token to the current keycloak token before fetch", async () => {
    const response = new Response("{}");
    const keycloakFetch = vi.fn().mockResolvedValue(response);
    const keycloak = {
      token: "current-token",
      fetch: keycloakFetch,
    } as unknown as Keycloak;

    const fetchImpl = createKeycloakFetch(keycloak);
    await fetchImpl("http://example.test/admin", {
      headers: { Authorization: "Bearer stale-token" },
    });

    expect(keycloakFetch).toHaveBeenCalledOnce();
    const [, init] = keycloakFetch.mock.calls[0];
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer current-token",
    );
  });

  it("does not add Authorization when the request had none", async () => {
    const response = new Response("{}");
    const keycloakFetch = vi.fn().mockResolvedValue(response);
    const keycloak = {
      token: "current-token",
      fetch: keycloakFetch,
    } as unknown as Keycloak;

    const fetchImpl = createKeycloakFetch(keycloak);
    await fetchImpl("http://example.test/public");

    const [, init] = keycloakFetch.mock.calls[0];
    expect(new Headers(init?.headers).has("Authorization")).toBe(false);
  });

  it("does not rewrite a non-Bearer Authorization header", async () => {
    const response = new Response("{}");
    const keycloakFetch = vi.fn().mockResolvedValue(response);
    const keycloak = {
      token: "current-token",
      fetch: keycloakFetch,
    } as unknown as Keycloak;

    const fetchImpl = createKeycloakFetch(keycloak);
    await fetchImpl("http://example.test/admin", {
      headers: { Authorization: "DPoP other-token" },
    });

    const [, init] = keycloakFetch.mock.calls[0];
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "DPoP other-token",
    );
  });
});
