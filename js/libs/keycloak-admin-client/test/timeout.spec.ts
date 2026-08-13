import { expect } from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";
import { Server, createServer } from "node:http";

describe("Timeout", () => {
  let server: Server;

  before(async () => {
    server = createServer((req, res) => {
      res.writeHead(200, { "Content-Type": "text/plain" });
      setTimeout(() => res.end("Hello, world!\n"), 1500);
    });
    server.listen(8888, "localhost");
  });

  after(async () => {
    await server[Symbol.asyncDispose]();
  });

  void it("create without timeout", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8888",
    });

    try {
      await client.auth(credentials);
    } catch (error) {
      expect(error).to.be.an("Error");
      expect((error as Error).message).to.contain("Unexpected token 'H'");
      return;
    }
    expect.fail(null, null, "auth did not fail");
  });

  void it("create with timeout", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8888",
      timeout: 1000,
    });

    try {
      await client.auth(credentials);
    } catch (error) {
      expect(error).to.be.an("DOMException");
      expect((error as DOMException).message).to.contain(
        "The operation was aborted due to timeout",
      );
      return;
    }
    expect.fail(null, null, "auth did not fail");
  });
});
