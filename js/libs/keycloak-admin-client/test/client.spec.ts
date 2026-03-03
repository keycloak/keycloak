import { expect } from "chai";
import { createServer, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { KeycloakAdminClient } from "../src/client.js";

const createToken = (payload: object = { exp: 9999999999 }) => {
  const header = Buffer.from(
    JSON.stringify({ alg: "HS256", typ: "JWT" }),
  ).toString("base64url");
  const tokenPayload = Buffer.from(JSON.stringify(payload)).toString(
    "base64url",
  );
  return `${header}.${tokenPayload}.signature`;
};

describe("KeycloakAdminClient", () => {
  it("does not throw when refresh token is undefined", () => {
    const client = new KeycloakAdminClient();

    expect(() => client.setRefreshToken(undefined)).to.not.throw();
    expect(client.refreshToken).to.be.undefined;
    expect(client.isRefreshTokenExpired()).to.be.false;
  });

  it("can clear a previously set refresh token", () => {
    const client = new KeycloakAdminClient();
    client.setRefreshToken(createToken());

    expect(client.refreshToken).to.be.a("string");

    client.setRefreshToken(undefined);

    expect(client.refreshToken).to.be.undefined;
    expect(client.isRefreshTokenExpired()).to.be.false;
  });

  it("auth succeeds for client_credentials responses without refresh_token", async () => {
    let server: Server | undefined;
    try {
      server = createServer((req, res) => {
        if (req.method !== "POST") {
          res.writeHead(405).end();
          return;
        }

        if (
          req.url !== "/realms/master/protocol/openid-connect/token" &&
          req.url !== "/realms/master/protocol/openid-connect/token/"
        ) {
          res.writeHead(404).end();
          return;
        }

        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(
          JSON.stringify({
            access_token: createToken(),
            expires_in: "300",
            refresh_expires_in: 0,
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

      const { port } = server.address() as AddressInfo;
      const client = new KeycloakAdminClient({
        baseUrl: `http://127.0.0.1:${port}`,
      });

      await client.auth({
        grantType: "client_credentials",
        clientId: "admin-cli",
        clientSecret: "secret",
      });

      expect(client.accessToken).to.be.a("string");
      expect(client.refreshToken).to.be.undefined;
    } finally {
      if (server) {
        await new Promise<void>((resolve, reject) => {
          server.close((error) => {
            if (error) {
              reject(error);
              return;
            }
            resolve();
          });
        });
      }
    }
  });
});
