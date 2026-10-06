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
});
