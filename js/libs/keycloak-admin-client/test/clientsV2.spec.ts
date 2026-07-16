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

    const patchedClient = await kcAdminClient.clients
      .v2()
      .byId(currentClientId)
      .patch({ additionalData: { displayName: patchedDisplayName } });

    expect((patchedClient as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );

    // Verify the change persisted
    const client = await kcAdminClient.clients.v2().byId(currentClientId).get();

    expect((client as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );
  });

  it("should filter and sort clients", async () => {
    const clientId1 = faker.internet.username();
    const clientId2 = faker.internet.username();

    // Create two clients using v2 API
    await kcAdminClient.clients.v2().post({
      clientId: clientId1,
      protocol: "openid-connect",
      description: "Client 1",
      enabled: true,
    });

    await kcAdminClient.clients.v2().post({
      clientId: clientId2,
      protocol: "openid-connect",
      enabled: true,
    });

    // Verify we can get them via v2 API
    const clients = await kcAdminClient.clients.v2().get();
    expect(clients).to.be.ok;
    expect(clients).to.be.an("array");

    const client1 = clients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId1,
    );
    expect(client1).to.be.ok;

    const client2 = clients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId2,
    );
    expect(client2).to.be.ok;

    // Sort by clientId
    const sortedClients = await kcAdminClient.clients.v2().get({
      queryParameters: {
        sort: "clientId",
      },
    });
    expect(sortedClients).to.be.ok;
    expect(sortedClients).to.be.an("array");
    const clientIds = sortedClients!.map(
      (c) => (c as OIDCClientRepresentation).clientId,
    );
    expect([...clientIds].sort()).to.deep.equal(clientIds);

    const sortedClient1 = sortedClients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId1,
    );
    expect(sortedClient1).to.be.ok;

    const sortedClient2 = sortedClients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId2,
    );
    expect(sortedClient2).to.be.ok;

    // Filter by clientId
    const filteredClients = await kcAdminClient.clients.v2().get({
      queryParameters: {
        fields: ["clientId"],
        q: `clientId eq "${clientId1}"`,
      },
    });
    expect(filteredClients).to.be.ok;
    expect(filteredClients).to.be.an("array");
    expect(filteredClients!.length).to.equal(1);
    expect(filteredClients![0].description).to.be.undefined;

    const filteredClient1 = filteredClients!.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId1,
    );
    expect(filteredClient1).to.be.ok;
    await kcAdminClient.clients.v2().byId(clientId1).delete();
    await kcAdminClient.clients.v2().byId(clientId2).delete();
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
