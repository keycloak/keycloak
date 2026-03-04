import * as chai from "chai";
import { fetchWithError } from "../src/utils/fetchWithError.js";

const expect = chai.expect;

describe("fetchWithError", () => {
  const originalFetch = globalThis.fetch;
  const originalWarn = console.warn;

  afterEach(() => {
    globalThis.fetch = originalFetch;
    console.warn = originalWarn;
  });

  it("warns once per deprecated admin endpoint", async () => {
    const warnings: string[] = [];
    console.warn = ((message: string) => {
      warnings.push(message);
    }) as typeof console.warn;

    globalThis.fetch = async () =>
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          "content-type": "application/json",
          Deprecation: "true",
          Sunset: "Wed, 11 Jan 2028 23:59:59 GMT",
        },
      });

    await fetchWithError("http://127.0.0.1:8180/admin/realms/master");
    await fetchWithError("http://127.0.0.1:8180/admin/realms/master");

    expect(warnings).to.have.length(1);
    expect(warnings[0]).to.contain(
      "Using deprecated Keycloak Admin endpoint: /admin/realms/master",
    );
    expect(warnings[0]).to.contain("Deprecation: true");
    expect(warnings[0]).to.contain("Sunset: Wed, 11 Jan 2028 23:59:59 GMT");
  });

  it("does not warn for non-admin endpoints", async () => {
    const warnings: string[] = [];
    console.warn = ((message: string) => {
      warnings.push(message);
    }) as typeof console.warn;

    globalThis.fetch = async () =>
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          "content-type": "application/json",
          Deprecation: "true",
        },
      });

    await fetchWithError(
      "http://127.0.0.1:8180/realms/master/protocol/openid-connect/token",
    );

    expect(warnings).to.have.length(0);
  });
});
