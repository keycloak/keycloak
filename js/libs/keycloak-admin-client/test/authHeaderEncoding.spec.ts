import { expect } from "chai";
import { createServer, type IncomingMessage, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { getToken } from "../src/utils/auth.js";

const encodeFormURIComponent = (data: string) =>
  encodeURIComponent(data)
    .replace(
      /[!'()*]/g,
      (c) => `%${c.charCodeAt(0).toString(16).toUpperCase()}`,
    )
    .replaceAll("%20", "+");

const toBasicHeader = (clientId: string, clientSecret: string) => {
  const username = encodeFormURIComponent(clientId);
  const password = encodeFormURIComponent(clientSecret);
  return `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`;
};

const startTokenServer = async (
  onRequest: (req: IncomingMessage) => void,
): Promise<Server> => {
  const server = createServer((req, res) => {
    onRequest(req);

    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(
      JSON.stringify({
        access_token: "access.token.value",
        expires_in: "300",
        refresh_expires_in: 1800,
        refresh_token: "refresh.token.value",
        token_type: "Bearer",
        not_before_policy: 0,
        session_state: "state",
        scope: "profile email",
      }),
    );
  });

  await new Promise<void>((resolve) => {
    server.listen(0, "127.0.0.1", resolve);
  });

  return server;
};

describe("Authorization header encoding", () => {
  it("encodes special characters in client credentials before base64", async () => {
    let authHeader = "";
    const server = await startTokenServer((req) => {
      authHeader = req.headers.authorization ?? "";
    });

    try {
      const { port } = server.address() as AddressInfo;

      await getToken({
        baseUrl: `http://127.0.0.1:${port}`,
        realmName: "master",
        credentials: {
          grantType: "client_credentials",
          clientId: "client id:with/slash",
          clientSecret: "secret !*'() +:/?",
        },
      });

      expect(authHeader).to.equal(
        toBasicHeader(
          "client id:with/slash",
          "secret !*'() +:/?",
        ),
      );
    } finally {
      await server[Symbol.asyncDispose]();
    }
  });
});
