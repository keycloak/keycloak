import { expect } from "chai";
import { once } from "node:events";
import { Server, createServer } from "node:http";
import type { AddressInfo } from "node:net";
import { KeycloakAdminClient } from "../src/client.js";

const credentials = {
  grantType: "client_credentials",
  clientId: "admin-cli",
  clientSecret: "secret",
} as const;

const token = (secondsFromNow: number) => {
  const payload = { exp: Math.ceil(Date.now() / 1000) + secondsFromNow };
  return `header.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.signature`;
};

describe("Client", () => {
  let server: Server;
  let baseUrl: string;
  let responses: object[];

  before(async () => {
    server = createServer((_req, res) => {
      const response = responses.shift();
      res.writeHead(response ? 200 : 500, {
        "content-type": "application/json",
      });
      res.end(JSON.stringify(response ?? { error: "No response queued." }));
    });
    server.listen(0, "localhost");
    await once(server, "listening");
    baseUrl = `http://localhost:${(server.address() as AddressInfo).port}`;
  });

  after(async () => {
    server.closeAllConnections();
    server.close();
    await once(server, "close");
  });

  // A client_credentials grant does not return a refresh token by default.
  void it("authenticates when the token response has no refresh token", async () => {
    responses = [{ access_token: token(60) }];

    const client = new KeycloakAdminClient({ baseUrl });
    await client.auth(credentials);

    expect(client.refreshToken).to.be.undefined;
    expect(client.isRefreshTokenExpired()).to.be.false;
  });

  void it("keeps the refresh token when the refreshed token response omits one", async () => {
    const refreshToken = token(600);
    const renewedAccessToken = token(60);
    responses = [
      { access_token: token(-60), refresh_token: refreshToken },
      { access_token: renewedAccessToken },
    ];

    const client = new KeycloakAdminClient({ baseUrl });
    await client.auth(credentials);

    expect(await client.getAccessToken()).to.equal(renewedAccessToken);
    expect(client.refreshToken).to.equal(refreshToken);
  });

  describe("custom fetch", () => {
    type FetchCall = {
      input: RequestInfo | URL;
      init?: RequestInit;
    };

    const jsonResponse = (body: unknown = {}) =>
      new Response(JSON.stringify(body), {
        status: 200,
        headers: { "content-type": "application/json" },
      });

    const createRecordingFetch = () => {
      const calls: FetchCall[] = [];
      const fetchImpl: typeof fetch = async (input, init) => {
        calls.push({ input, init });
        return jsonResponse([]);
      };
      return { calls, fetchImpl };
    };

    const withToken = (client: KeycloakAdminClient) => {
      client.registerTokenProvider({
        getAccessToken: async () => "access-token",
      });
      return client;
    };

    it("uses a fetch injected via the constructor for v1 requests", async () => {
      const { calls, fetchImpl } = createRecordingFetch();
      const client = withToken(
        new KeycloakAdminClient({
          baseUrl: "http://example.test",
          realmName: "test",
          requestOptions: { headers: { "X-Custom-Header": "custom" } },
          fetch: fetchImpl,
        }),
      );

      await client.serverInfo.find();

      expect(calls).to.have.length(1);
      expect(String(calls[0].input)).to.equal(
        "http://example.test/admin/serverinfo",
      );
      expect(
        new Headers(calls[0].init?.headers).get("X-Custom-Header"),
      ).to.equal("custom");
      expect(new Headers(calls[0].init?.headers).get("Authorization")).to.equal(
        "Bearer access-token",
      );
    });

    it("uses a fetch injected via setConfig for v1 requests", async () => {
      const { calls, fetchImpl } = createRecordingFetch();
      const client = withToken(
        new KeycloakAdminClient({
          baseUrl: "http://example.test",
          realmName: "test",
        }),
      );
      client.setConfig({ fetch: fetchImpl });

      await client.serverInfo.find();

      expect(calls).to.have.length(1);
      expect(String(calls[0].input)).to.equal(
        "http://example.test/admin/serverinfo",
      );
    });

    it("uses a fetch injected via the constructor for v2 requests", async () => {
      const { calls, fetchImpl } = createRecordingFetch();
      const client = withToken(
        new KeycloakAdminClient({
          baseUrl: "http://example.test",
          realmName: "test",
          enableExperimentalApis: true,
          fetch: fetchImpl,
        }),
      );

      await client.clients.v2().get();

      expect(calls).to.have.length(1);
      expect(String(calls[0].input)).to.include("/admin/api/test/clients/v2");
    });

    it("falls back to the global fetch when none is configured", async () => {
      const originalFetch = globalThis.fetch;
      const { calls, fetchImpl } = createRecordingFetch();
      globalThis.fetch = fetchImpl;

      try {
        const client = withToken(
          new KeycloakAdminClient({
            baseUrl: "http://example.test",
            realmName: "test",
          }),
        );

        await client.serverInfo.find();

        expect(calls).to.have.length(1);
        expect(client.getFetch()).to.equal(globalThis.fetch);
      } finally {
        globalThis.fetch = originalFetch;
      }
    });
  });
});
