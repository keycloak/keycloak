import * as chai from "chai";
import { Server, createServer } from "node:http";
import { getToken } from "../src/utils/auth.js";

const expect = chai.expect;

describe("Token exchange", () => {
  let server: Server;
  let lastAuthorizationHeader: string | null = null;
  let lastBody = "";

  before(async () => {
    server = createServer((req, res) => {
      const chunks: Buffer[] = [];

      req.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
      req.on("end", () => {
        lastAuthorizationHeader = req.headers.authorization ?? null;
        lastBody = Buffer.concat(chunks).toString("utf-8");

        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(
          JSON.stringify({
            access_token: "access-token",
            expires_in: 300,
            refresh_expires_in: 0,
            refresh_token: "refresh-token",
            token_type: "Bearer",
            not_before_policy: 0,
            session_state: "session-id",
            scope: "profile email",
          }),
        );
      });
    });

    await new Promise<void>((resolve) =>
      server.listen(8890, "127.0.0.1", resolve),
    );
  });

  after(async () => {
    await server[Symbol.asyncDispose]();
  });

  it("sends token exchange payload with default subject token type", async () => {
    await getToken({
      baseUrl: "http://127.0.0.1:8890",
      realmName: "master",
      credentials: {
        grantType: "urn:ietf:params:oauth:grant-type:token-exchange",
        clientId: "admin-cli",
        clientSecret: "test-secret",
        subjectToken: "subject-token",
        requestedSubject: "impersonated-user",
        audience: "target-client",
      },
    });

    const body = new URLSearchParams(lastBody);
    expect(body.get("grant_type")).to.equal(
      "urn:ietf:params:oauth:grant-type:token-exchange",
    );
    expect(body.get("subject_token")).to.equal("subject-token");
    expect(body.get("requested_subject")).to.equal("impersonated-user");
    expect(body.get("audience")).to.equal("target-client");
    expect(body.get("subject_token_type")).to.equal(
      "urn:ietf:params:oauth:token-type:access_token",
    );
    expect(lastAuthorizationHeader).to.match(/^Basic /);
  });

  it("sends optional token exchange parameters", async () => {
    await getToken({
      baseUrl: "http://127.0.0.1:8890",
      realmName: "master",
      credentials: {
        grantType: "urn:ietf:params:oauth:grant-type:token-exchange",
        clientId: "admin-cli",
        subjectToken: "subject-token",
        subjectTokenType: "urn:custom:token-type",
        requestedTokenType: "urn:ietf:params:oauth:token-type:refresh_token",
        actorToken: "actor-token",
        actorTokenType: "urn:ietf:params:oauth:token-type:access_token",
        subjectIssuer: "external-idp",
        audience: ["service-a", "service-b"],
        resource: ["resource-a", "resource-b"],
      },
    });

    const body = new URLSearchParams(lastBody);
    expect(body.get("subject_token_type")).to.equal("urn:custom:token-type");
    expect(body.get("requested_token_type")).to.equal(
      "urn:ietf:params:oauth:token-type:refresh_token",
    );
    expect(body.get("actor_token")).to.equal("actor-token");
    expect(body.get("actor_token_type")).to.equal(
      "urn:ietf:params:oauth:token-type:access_token",
    );
    expect(body.get("subject_issuer")).to.equal("external-idp");
    expect(body.getAll("audience")).to.deep.equal(["service-a", "service-b"]);
    expect(body.getAll("resource")).to.deep.equal(["resource-a", "resource-b"]);
  });
});
