// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type ComponentRepresentation from "../src/defs/componentRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("User federation using component api", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentUserFed: ComponentRepresentation;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);

    // create user fed
    const name = faker.internet.username();
    const component = await kcAdminClient.components.create({
      name,
      parentId: "master",
      providerId: "ldap",
      providerType: "org.keycloak.storage.UserStorageProvider",
      config: {
        editMode: ["READ_ONLY"],
      },
    });
    expect(component.id).to.be.ok;

    // assign current user fed
    const fed = (await kcAdminClient.components.findOne({
      id: component.id,
    }))!;
    currentUserFed = fed;
  });

  after(async () => {
    await kcAdminClient.components.del({
      id: currentUserFed.id!,
    });

    // check deleted
    const idp = await kcAdminClient.components.findOne({
      id: currentUserFed.id!,
    });
    expect(idp).to.be.null;
  });

  it("list user federations", async () => {
    const feds = await kcAdminClient.components.find({
      parent: "master",
      type: "org.keycloak.storage.UserStorageProvider",
    });
    expect(feds.length).to.be.least(1);
  });

  it("get a user federation", async () => {
    const fed = await kcAdminClient.components.findOne({
      id: currentUserFed.id!,
    });
    expect(fed).to.include({
      id: currentUserFed.id,
    });
  });

  it("get a sub components", async () => {
    const list = await kcAdminClient.components.listSubComponents({
      id: currentUserFed.id!,
      type: "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
    });

    expect(list).to.be.ok;
  });

  it("update a user federation", async () => {
    await kcAdminClient.components.update(
      { id: currentUserFed.id! },
      {
        // parentId, providerId, providerType required for update
        parentId: "master",
        providerId: "ldap",
        providerType: "org.keycloak.storage.UserStorageProvider",
        name: "cool-name",
      },
    );
    const updated = await kcAdminClient.components.findOne({
      id: currentUserFed.id!,
    });

    expect(updated).to.include({
      id: currentUserFed.id,
      name: "cool-name",
    });
  });
});
