import { expect } from "chai";
import {
  createServer,
  type IncomingMessage,
  type ServerResponse,
} from "node:http";
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

const readBody = async (request: IncomingMessage) =>
  await new Promise<string>((resolve) => {
    const chunks: Buffer[] = [];
    request.on("data", (chunk: Buffer | string) => {
      chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    });
    request.on("end", () => {
      resolve(Buffer.concat(chunks).toString("utf8"));
    });
  });

const startServer = async (
  handler: (
    request: IncomingMessage,
    response: ServerResponse,
  ) => Promise<void>,
) => {
  const server = createServer((request, response) => {
    handler(request, response).catch((error: unknown) => {
      response.writeHead(500, { "content-type": "text/plain" });
      response.end(String(error));
    });
  });

  await new Promise<void>((resolve) => {
    server.listen(0, "127.0.0.1", resolve);
  });

  const { port } = server.address() as AddressInfo;
  const close = async () =>
    await new Promise<void>((resolve, reject) => {
      server.close((error) => {
        if (error) {
          reject(error);
          return;
        }
        resolve();
      });
    });

  return {
    close,
    server,
    url: `http://127.0.0.1:${port}`,
  };
};

const createAuthenticatedClient = (baseUrl: string) => {
  const client = new KeycloakAdminClient({
    baseUrl,
  });
  client.setAccessToken(createToken());
  return client;
};

describe("Account resource", () => {
  it("gets account profile with userProfileMetadata query", async () => {
    const server = await startServer(async (request, response) => {
      const url = new URL(request.url ?? "/", "http://localhost");
      expect(request.method).to.equal("GET");
      expect(url.pathname).to.match(/^\/realms\/master\/account\/?$/);
      expect(url.searchParams.get("userProfileMetadata")).to.equal("true");
      expect(request.headers.authorization).to.match(/^Bearer /);

      response.writeHead(200, { "content-type": "application/json" });
      response.end(JSON.stringify({ username: "demo-user" }));
    });

    try {
      const client = createAuthenticatedClient(server.url);
      const profile = await client.account.getProfile({
        userProfileMetadata: true,
      });
      expect(profile.username).to.equal("demo-user");
    } finally {
      await server.close();
    }
  });

  it("updates account profile using POST /realms/{realm}/account", async () => {
    const server = await startServer(async (request, response) => {
      const url = new URL(request.url ?? "/", "http://localhost");
      expect(request.method).to.equal("POST");
      expect(url.pathname).to.match(/^\/realms\/master\/account\/?$/);
      expect(request.headers.authorization).to.match(/^Bearer /);

      const body = await readBody(request);
      expect(JSON.parse(body)).to.deep.equal({
        firstName: "John",
        lastName: "Doe",
      });

      response.writeHead(204);
      response.end();
    });

    try {
      const client = createAuthenticatedClient(server.url);
      await client.account.updateProfile({
        firstName: "John",
        lastName: "Doe",
      });
    } finally {
      await server.close();
    }
  });

  it("sets credential label using JSON string payload", async () => {
    const server = await startServer(async (request, response) => {
      const url = new URL(request.url ?? "/", "http://localhost");
      expect(request.method).to.equal("PUT");
      expect(url.pathname).to.equal(
        "/realms/master/account/credentials/cred-1/label",
      );
      expect(request.headers.authorization).to.match(/^Bearer /);
      expect(request.headers["content-type"]).to.include("application/json");

      const body = await readBody(request);
      expect(body).to.equal('"My label"');

      response.writeHead(204);
      response.end();
    });

    try {
      const client = createAuthenticatedClient(server.url);
      await client.account.setCredentialLabel(
        { credentialId: "cred-1" },
        "My label",
      );
    } finally {
      await server.close();
    }
  });

  it("lists linked accounts with query params", async () => {
    const server = await startServer(async (request, response) => {
      const url = new URL(request.url ?? "/", "http://localhost");
      expect(request.method).to.equal("GET");
      expect(url.pathname).to.match(
        /^\/realms\/master\/account\/linked-accounts\/?$/,
      );
      expect(url.searchParams.get("linked")).to.equal("true");
      expect(url.searchParams.get("search")).to.equal("github");
      expect(url.searchParams.get("first")).to.equal("1");
      expect(url.searchParams.get("max")).to.equal("10");
      expect(request.headers.authorization).to.match(/^Bearer /);

      response.writeHead(200, { "content-type": "application/json" });
      response.end(
        JSON.stringify([
          {
            connected: true,
            providerAlias: "github",
          },
        ]),
      );
    });

    try {
      const client = createAuthenticatedClient(server.url);
      const linkedAccounts = await client.account.listLinkedAccounts({
        linked: true,
        search: "github",
        first: 1,
        max: 10,
      });

      expect(linkedAccounts).to.have.length(1);
      expect(linkedAccounts[0].providerAlias).to.equal("github");
      expect(linkedAccounts[0].connected).to.equal(true);
    } finally {
      await server.close();
    }
  });
});
