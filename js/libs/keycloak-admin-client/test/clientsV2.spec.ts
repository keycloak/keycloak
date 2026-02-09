import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type { OIDCClientRepresentation } from "../src/generated/models/index.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Clients V2 API", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentClientId: string;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient({
      enableExperimentalApis: true,
    });
    await kcAdminClient.auth(credentials);

    // Create a client for testing using v2 API
    currentClientId = faker.internet.username();
    await kcAdminClient.clients.v2().post({
      clientId: currentClientId,
      protocol: "openid-connect",
      enabled: true,
    });
  });

  after(async () => {
    // Delete the test client
    if (currentClientId) {
      await kcAdminClient.clients.v2().byId(currentClientId).delete();
    }
  });

  it("should list clients", async () => {
    const clients = await kcAdminClient.clients.v2().get();

    expect(clients).to.be.ok;
    expect(clients).to.be.an("array");
    expect(clients!.length).to.be.greaterThan(0);

    // Verify our test client is in the list
    const testClient = clients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === currentClientId,
    );
    expect(testClient).to.be.ok;
  });

  it("should get a single client by clientId", async () => {
    const client = await kcAdminClient.clients.v2().byId(currentClientId).get();

    expect(client).to.be.ok;
    expect((client as OIDCClientRepresentation).clientId).to.equal(
      currentClientId,
    );
  });

  it("should update a client with PUT", async () => {
    const updatedDescription = "Updated via V2 API test";

    await kcAdminClient.clients.v2().byId(currentClientId).put({
      clientId: currentClientId,
      protocol: "openid-connect",
      description: updatedDescription,
    });

    const client = await kcAdminClient.clients.v2().byId(currentClientId).get();

    expect((client as OIDCClientRepresentation).description).to.equal(
      updatedDescription,
    );
  });

  it("should patch a client", async () => {
    const patchedDisplayName = "Patched Display Name";

    // Note: Kiota's patch expects an ArrayBuffer for merge-patch+json
    const patchBody = JSON.stringify({ displayName: patchedDisplayName });
    const encoder = new TextEncoder();
    const patchBuffer = encoder.encode(patchBody).buffer;

    const patchedClient = await kcAdminClient.clients
      .v2()
      .byId(currentClientId)
      .patch(patchBuffer);

    expect((patchedClient as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );

    // Verify the change persisted
    const client = await kcAdminClient.clients.v2().byId(currentClientId).get();

    expect((client as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );
  });

  it("should create and delete a client", async () => {
    const clientId = faker.internet.username();

    // Create a new client using v2 API
    await kcAdminClient.clients.v2().post({
      clientId,
      protocol: "openid-connect",
      enabled: true,
      description: "Test client for deletion",
    });

    // Verify we can get it via v2 API
    const client = await kcAdminClient.clients.v2().byId(clientId).get();
    expect((client as OIDCClientRepresentation).clientId).to.equal(clientId);

    // Delete the client using v2 API
    await kcAdminClient.clients.v2().byId(clientId).delete();

    // Verify it's deleted by checking it's no longer in the list
    const clients = await kcAdminClient.clients.v2().get();

    const deletedClient = clients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId,
    );
    expect(deletedClient).to.be.undefined;
  });

  it("should create an OIDC client with full configuration", async () => {
    const clientId = `full-config-${faker.internet.username()}`;

    await kcAdminClient.clients.v2().post({
      clientId,
      protocol: "openid-connect",
      enabled: true,
      displayName: "Full Config Test Client",
      description: "A client with full OIDC configuration",
      redirectUris: ["http://localhost:3000/callback"],
      webOrigins: ["http://localhost:3000"],
    });

    // Get via v2 API and verify
    const client = await kcAdminClient.clients.v2().byId(clientId).get();

    expect(client).to.be.ok;
    expect((client as OIDCClientRepresentation).displayName).to.equal(
      "Full Config Test Client",
    );
    expect((client as OIDCClientRepresentation).protocol).to.equal(
      "openid-connect",
    );

    // Cleanup
    await kcAdminClient.clients.v2().byId(clientId).delete();
  });
});
