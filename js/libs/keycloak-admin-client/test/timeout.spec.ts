import { expect } from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";
import { Server, createServer } from "node:http";
import type { AddressInfo } from "node:net";

describe("Timeout", () => {
  let server: Server;
  let baseUrl: string;

  before(async () => {
    server = createServer((req, res) => {
      res.writeHead(200, { "Content-Type": "text/plain" });
      setTimeout(() => res.end("Hello, world!\n"), 1500);
    });
    await new Promise<void>((resolve) =>
      server.listen(0, "localhost", resolve),
    );
    const { port } = server.address() as AddressInfo;
    baseUrl = `http://localhost:${port}`;
  });

  after(async () => {
    await server[Symbol.asyncDispose]();
  });

  void it("create without timeout", async () => {
    const client = new KeycloakAdminClient({ baseUrl });

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
    const client = new KeycloakAdminClient({ baseUrl, timeout: 1000 });

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
