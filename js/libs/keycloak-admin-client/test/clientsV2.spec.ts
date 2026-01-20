import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type { ClientsV2Api } from "../src/resources/clientsV2.js";
import type { OIDCClientRepresentation } from "../src/generated/models/index.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Clients V2 API", () => {
  let kcAdminClient: KeycloakAdminClient;
  let clientsV2Api: ClientsV2Api;
  let currentClientId: string;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);

    // Get the v2 API instance
    clientsV2Api = await kcAdminClient.clients.v2.api();

    // Create a client for testing using v2 API
    currentClientId = faker.internet.username();
    await clientsV2Api.adminApiRealmNameClientsVersionPost({
      realmName: kcAdminClient.realmName,
      version: "v2",
      adminApiRealmNameClientsVersionGet200ResponseInner: {
        clientId: currentClientId,
        protocol: "openid-connect",
        enabled: true,
      },
    });
  });

  after(async () => {
    // Delete the test client
    if (currentClientId) {
      await clientsV2Api.adminApiRealmNameClientsVersionIdDelete({
        realmName: kcAdminClient.realmName,
        version: "v2",
        id: currentClientId,
      });
    }
  });

  it("should list clients", async () => {
    const clients = await clientsV2Api.adminApiRealmNameClientsVersionGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
    });

    expect(clients).to.be.ok;
    expect(clients).to.be.an("array");
    expect(clients.length).to.be.greaterThan(0);

    // Verify our test client is in the list
    const testClient = clients.find(
      (c) => (c as OIDCClientRepresentation).clientId === currentClientId,
    );
    expect(testClient).to.be.ok;
  });

  it("should get a single client by clientId", async () => {
    const client = await clientsV2Api.adminApiRealmNameClientsVersionIdGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: currentClientId,
    });

    expect(client).to.be.ok;
    expect((client as OIDCClientRepresentation).clientId).to.equal(
      currentClientId,
    );
  });

  it("should update a client with PUT", async () => {
    const updatedDescription = "Updated via V2 API test";

    await clientsV2Api.adminApiRealmNameClientsVersionIdPut({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: currentClientId,
      adminApiRealmNameClientsVersionGet200ResponseInner: {
        clientId: currentClientId,
        protocol: "openid-connect",
        description: updatedDescription,
      },
    });

    const client = await clientsV2Api.adminApiRealmNameClientsVersionIdGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: currentClientId,
    });

    expect((client as OIDCClientRepresentation).description).to.equal(
      updatedDescription,
    );
  });

  it("should patch a client", async () => {
    const patchedDisplayName = "Patched Display Name";

    const patchedClient =
      await clientsV2Api.adminApiRealmNameClientsVersionIdPatch({
        realmName: kcAdminClient.realmName,
        version: "v2",
        id: currentClientId,
        requestBody: {
          displayName: patchedDisplayName,
        },
      });

    expect((patchedClient as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );

    // Verify the change persisted
    const client = await clientsV2Api.adminApiRealmNameClientsVersionIdGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: currentClientId,
    });

    expect((client as OIDCClientRepresentation).displayName).to.equal(
      patchedDisplayName,
    );
  });

  it("should create and delete a client", async () => {
    const clientId = faker.internet.username();

    // Create a new client using v2 API
    await clientsV2Api.adminApiRealmNameClientsVersionPost({
      realmName: kcAdminClient.realmName,
      version: "v2",
      adminApiRealmNameClientsVersionGet200ResponseInner: {
        clientId,
        protocol: "openid-connect",
        enabled: true,
        description: "Test client for deletion",
      },
    });

    // Verify we can get it via v2 API
    const client = await clientsV2Api.adminApiRealmNameClientsVersionIdGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: clientId,
    });
    expect((client as OIDCClientRepresentation).clientId).to.equal(clientId);

    // Delete the client using v2 API
    await clientsV2Api.adminApiRealmNameClientsVersionIdDelete({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: clientId,
    });

    // Verify it's deleted by checking it's no longer in the list
    const clients = await clientsV2Api.adminApiRealmNameClientsVersionGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
    });

    const deletedClient = clients.find(
      (c) => (c as OIDCClientRepresentation).clientId === clientId,
    );
    expect(deletedClient).to.be.undefined;
  });

  it("should create an OIDC client with full configuration", async () => {
    const clientId = `full-config-${faker.internet.username()}`;

    await clientsV2Api.adminApiRealmNameClientsVersionPost({
      realmName: kcAdminClient.realmName,
      version: "v2",
      adminApiRealmNameClientsVersionGet200ResponseInner: {
        clientId,
        protocol: "openid-connect",
        enabled: true,
        displayName: "Full Config Test Client",
        description: "A client with full OIDC configuration",
        redirectUris: new Set(["http://localhost:3000/callback"]),
        webOrigins: new Set(["http://localhost:3000"]),
      },
    });

    // Get via v2 API and verify
    const client = await clientsV2Api.adminApiRealmNameClientsVersionIdGet({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: clientId,
    });

    expect(client).to.be.ok;
    expect((client as OIDCClientRepresentation).displayName).to.equal(
      "Full Config Test Client",
    );
    expect((client as OIDCClientRepresentation).protocol).to.equal(
      "openid-connect",
    );

    // Cleanup
    await clientsV2Api.adminApiRealmNameClientsVersionIdDelete({
      realmName: kcAdminClient.realmName,
      version: "v2",
      id: clientId,
    });
  });
});
