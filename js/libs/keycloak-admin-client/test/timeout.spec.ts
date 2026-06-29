import { expect } from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";
import { type AddressInfo, Server, createServer } from "node:http";

describe("Timeout", () => {
  let server: Server;
  let port: number;

  before(async () => {
    server = createServer((req, res) => {
      res.writeHead(200, { "Content-Type": "text/plain" });
      setTimeout(() => res.end("Hello, world!\n"), 1500);
    });
    await new Promise<void>((resolve) =>
      server.listen(0, "127.0.0.1", () => {
        port = (server.address() as AddressInfo).port;
        resolve();
      }),
    );
  });

  after(async () => {
    await new Promise<void>((resolve, reject) =>
      server.close((error) => (error ? reject(error) : resolve())),
    );
  });

  void it("create without timeout", async () => {
    const invalidJsonServer = createServer((req, res) => {
      res.writeHead(200, { "Content-Type": "text/plain" });
      res.end("Hello, world!\n");
    });
    const invalidJsonPort = await new Promise<number>((resolve) =>
      invalidJsonServer.listen(0, "127.0.0.1", () =>
        resolve((invalidJsonServer.address() as AddressInfo).port),
      ),
    );

    const client = new KeycloakAdminClient({
      baseUrl: `http://127.0.0.1:${invalidJsonPort}`,
    });

    try {
      await client.auth(credentials);
    } catch (error) {
      expect(error).to.be.an("Error");
      expect((error as Error).message).to.contain("Unexpected token 'H'");
      await new Promise<void>((resolve, reject) =>
        invalidJsonServer.close((closeError) =>
          closeError ? reject(closeError) : resolve(),
        ),
      );
      return;
    }

    await new Promise<void>((resolve, reject) =>
      invalidJsonServer.close((closeError) =>
        closeError ? reject(closeError) : resolve(),
      ),
    );
    expect.fail(null, null, "auth did not fail");
  });

  void it("create with timeout", async () => {
    const client = new KeycloakAdminClient({
      baseUrl: `http://127.0.0.1:${port}`,
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
