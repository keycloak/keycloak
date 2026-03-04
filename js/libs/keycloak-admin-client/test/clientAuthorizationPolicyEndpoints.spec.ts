import { expect } from "chai";
import { Server, createServer } from "node:http";
import { KeycloakAdminClient } from "../src/client.js";

describe("Client authorization policy endpoints", () => {
  let server: Server;
  const requestUrls: string[] = [];

  before(async () => {
    server = createServer((req, res) => {
      requestUrls.push(req.url ?? "");
      const payload = {
        id: "policy-id",
        name: "policy-name",
      };

      if (req.url?.includes("/policy/role/")) {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ ...payload, type: "role" }));
        return;
      }

      if (req.url?.includes("/policy/user/")) {
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ ...payload, type: "user" }));
        return;
      }

      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ...payload, type: "generic" }));
    });
    await new Promise<void>((resolve) =>
      server.listen(8889, "127.0.0.1", resolve),
    );
  });

  after(async () => {
    await server[Symbol.asyncDispose]();
  });

  it("gets role policy by id", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8889",
    });

    const policy = await client.clients.findRolePolicy({
      id: "client-id",
      policyId: "policy-id",
    });

    expect(policy).to.include({ id: "policy-id", type: "role" });
    expect(requestUrls[requestUrls.length - 1]).to.equal(
      "/admin/realms/master/clients/client-id/authz/resource-server/policy/role/policy-id",
    );
  });

  it("gets policy by type and id", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8889",
    });

    const policy = await client.clients.findOnePolicyWithType({
      id: "client-id",
      type: "user",
      policyId: "policy-id",
    });

    expect(policy).to.include({ id: "policy-id", type: "user" });
    expect(requestUrls[requestUrls.length - 1]).to.equal(
      "/admin/realms/master/clients/client-id/authz/resource-server/policy/user/policy-id",
    );
  });

  it("gets policy by id", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8889",
    });

    const policy = await client.clients.findOnePolicy({
      id: "client-id",
      policyId: "policy-id",
    });

    expect(policy).to.include({ id: "policy-id", type: "generic" });
    expect(requestUrls[requestUrls.length - 1]).to.equal(
      "/admin/realms/master/clients/client-id/authz/resource-server/policy/policy-id",
    );
  });
});
