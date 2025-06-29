// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import { fail } from "assert";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type GroupRepresentation from "../src/defs/groupRepresentation.js";
import type { PartialImportRealmRepresentation } from "../src/defs/realmRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

const createRealm = async (kcAdminClient: KeycloakAdminClient) => {
  const realmId = faker.internet.username().toLowerCase();
  const realmName = faker.internet.username().toLowerCase();
  const realm = await kcAdminClient.realms.create({
    id: realmId,
    realm: realmName,
  });
  expect(realm.realmName).to.be.equal(realmName);

  return { realmId, realmName };
};

const deleteRealm = async (
  kcAdminClient: KeycloakAdminClient,
  currentRealmName: string,
) => {
  await kcAdminClient.realms.del({ realm: currentRealmName });
  const realm = await kcAdminClient.realms.findOne({
    realm: currentRealmName,
  });
  expect(realm).to.be.null;
};

describe("Realms", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentRealmId: string;
  let currentRealmName: string;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
  });

  it("list realms", async () => {
    const realms = await kcAdminClient.realms.find();
    expect(realms.length).to.be.least(1);
  });

  it("create realm", async () => {
    const realmId = faker.internet.username().toLowerCase();
    const realmName = faker.internet.username().toLowerCase();
    const realm = await kcAdminClient.realms.create({
      id: realmId,
      realm: realmName,
    });
    expect(realm.realmName).to.be.equal(realmName);
    currentRealmId = realmId;
    currentRealmName = realmName;
  });

  it("get a realm", async () => {
    const realm = await kcAdminClient.realms.findOne({
      realm: currentRealmName,
    });
    expect(realm).to.include({
      id: currentRealmId,
      realm: currentRealmName,
    });
  });

  const roleToImport: PartialImportRealmRepresentation = {
    ifResourceExists: "FAIL",
    roles: {
      realm: [
        {
          id: "9d2638c8-4c62-4c42-90ea-5f3c836d0cc8",
          name: "myRole",
          scopeParamRequired: false,
          composite: false,
        },
      ],
    },
  };

  it("does partial import", async () => {
    const result = await kcAdminClient.realms.partialImport({
      realm: currentRealmName,
      rep: roleToImport,
    });
    expect(result.added).to.be.eq(1);
    expect(result.overwritten).to.be.eq(0);
    expect(result.skipped).to.be.eq(0);
    expect(result.results.length).to.be.eq(1);
    expect(result.results[0].action).to.be.eq("ADDED");
    expect(result.results[0].resourceName).to.be.eq("myRole");
    expect(result.results[0].id).to.exist;
  });

  it("export a realm", async () => {
    const realm = await kcAdminClient.realms.export({
      realm: currentRealmName,
      exportClients: true,
      exportGroupsAndRoles: true,
    });
    expect(realm).to.include({
      id: currentRealmId,
      realm: currentRealmName,
    });
  });

  it("update a realm", async () => {
    await kcAdminClient.realms.update(
      { realm: currentRealmName },
      {
        displayName: "test",
      },
    );
    const realm = await kcAdminClient.realms.findOne({
      realm: currentRealmName,
    });
    expect(realm).to.include({
      id: currentRealmId,
      realm: currentRealmName,
      displayName: "test",
    });
  });

  it("client registration policy providers", async () => {
    const list =
      await kcAdminClient.realms.getClientRegistrationPolicyProviders({
        realm: currentRealmName,
      });

    expect(list).to.be.ok;
  });

  it("delete a realm", async () => {
    await kcAdminClient.realms.del({ realm: currentRealmName });
    const realm = await kcAdminClient.realms.findOne({
      realm: currentRealmName,
    });
    expect(realm).to.be.null;
  });

  describe("Realm Client Initial Access", () => {
    before(async () => {
      kcAdminClient = new KeycloakAdminClient();
      await kcAdminClient.auth(credentials);

      const created = await createRealm(kcAdminClient);
      currentRealmName = created.realmName;

      await kcAdminClient.realms.createClientsInitialAccess(
        { realm: currentRealmName },
        { count: 1, expiration: 0 },
      );
    });

    after(async () => {
      await deleteRealm(kcAdminClient, currentRealmName);
    });

    it("list client initial access", async () => {
      const initialAccess = await kcAdminClient.realms.getClientsInitialAccess({
        realm: currentRealmName,
      });
      expect(initialAccess).to.be.ok;
      expect(initialAccess[0].count).to.be.eq(1);
    });

    it("del client initial access", async () => {
      const access = await kcAdminClient.realms.createClientsInitialAccess(
        { realm: currentRealmName },
        { count: 1, expiration: 0 },
      );
      expect(
        (
          await kcAdminClient.realms.getClientsInitialAccess({
            realm: currentRealmName,
          })
        ).length,
      ).to.be.eq(2);

      await kcAdminClient.realms.delClientsInitialAccess({
        realm: currentRealmName,
        id: access.id!,
      });

      const initialAccess = await kcAdminClient.realms.getClientsInitialAccess({
        realm: currentRealmName,
      });
      expect(initialAccess).to.be.ok;
      expect(initialAccess[0].count).to.be.eq(1);
    });
  });

  describe("Realm default groups", () => {
    const groupName = "my-group";
    let currentGroup: GroupRepresentation;

    before(async () => {
      kcAdminClient = new KeycloakAdminClient();
      await kcAdminClient.auth(credentials);

      currentRealmName = (await createRealm(kcAdminClient)).realmName;
      currentGroup = await kcAdminClient.groups.create({
        name: groupName,
        realm: currentRealmName,
      });
    });

    after(async () => {
      await deleteRealm(kcAdminClient, currentRealmName);
    });

    it("add group to default groups", async () => {
      await kcAdminClient.realms.addDefaultGroup({
        id: currentGroup.id!,
        realm: currentRealmName,
      });

      const defaultGroups = await kcAdminClient.realms.getDefaultGroups({
        realm: currentRealmName,
      });

      expect(defaultGroups).to.be.ok;
      expect(defaultGroups.length).to.be.eq(1);
      expect(defaultGroups[0].id).to.be.eq(currentGroup.id);
    });

    it("get a group by its path name", async () => {
      const queriedGroup = await kcAdminClient.realms.getGroupByPath({
        realm: currentRealmName,
        path: groupName,
      });

      expect(queriedGroup).to.be.ok;
      expect(queriedGroup.id).to.be.eq(currentGroup.id);
    });

    it("remove group from default groups", async () => {
      await kcAdminClient.realms.removeDefaultGroup({
        id: currentGroup.id!,
        realm: currentRealmName,
      });

      const defaultGroups = await kcAdminClient.realms.getDefaultGroups({
        realm: currentRealmName,
      });

      expect(defaultGroups).to.be.ok;
      expect(defaultGroups.length).to.be.eq(0);
    });
  });

  describe("Realm Events", () => {
    before(async () => {
      kcAdminClient = new KeycloakAdminClient();
      await kcAdminClient.auth(credentials);

      const created = await createRealm(kcAdminClient);
      currentRealmId = created.realmId;
      currentRealmName = created.realmName;
    });

    it("get events config for a realm", async () => {
      const config = await kcAdminClient.realms.getConfigEvents({
        realm: currentRealmName,
      });

      expect(config).to.be.ok;
      expect(config.adminEventsEnabled).to.be.eq(false);
    });

    it("enable events", async () => {
      const config = await kcAdminClient.realms.getConfigEvents({
        realm: currentRealmName,
      });
      config.eventsEnabled = true;
      await kcAdminClient.realms.updateConfigEvents(
        { realm: currentRealmName },
        config,
      );

      const newConfig = await kcAdminClient.realms.getConfigEvents({
        realm: currentRealmName,
      });

      expect(newConfig).to.be.ok;
      expect(newConfig.eventsEnabled).to.be.eq(true);
    });

    it("list events of a realm", async () => {
      // @TODO: In order to test it, there have to be events
      const events = await kcAdminClient.realms.findEvents({
        realm: currentRealmName,
      });

      expect(events).to.be.ok;
    });

    it("list admin events of a realm", async () => {
      // @TODO: In order to test it, there have to be events
      const events = await kcAdminClient.realms.findAdminEvents({
        realm: currentRealmName,
      });

      expect(events).to.be.ok;
    });

    it("clear events", async () => {
      await kcAdminClient.realms.clearEvents({ realm: currentRealmName });
      await kcAdminClient.realms.clearAdminEvents({ realm: currentRealmName });

      const events = await kcAdminClient.realms.findAdminEvents({
        realm: currentRealmName,
      });

      expect(events).to.deep.eq([]);
    });

    after(async () => {
      await deleteRealm(kcAdminClient, currentRealmName);
    });
  });

  describe("Realm Users Management Permissions", () => {
    before(async () => {
      kcAdminClient = new KeycloakAdminClient();
      await kcAdminClient.auth(credentials);

      const created = await createRealm(kcAdminClient);
      currentRealmId = created.realmId;
      currentRealmName = created.realmName;
    });

    it("get users management permissions", async () => {
      const managementPermissions =
        await kcAdminClient.realms.getUsersManagementPermissions({
          realm: currentRealmName,
        });
      expect(managementPermissions).to.be.ok;
    });

    it.skip("enable users management permissions", async () => {
      const managementPermissions =
        await kcAdminClient.realms.updateUsersManagementPermissions({
          realm: currentRealmName,
          enabled: true,
        });
      expect(managementPermissions).to.include({ enabled: true });
    });

    it("get realm keys", async () => {
      const keys = await kcAdminClient.realms.getKeys({
        realm: currentRealmName,
      });
      expect(keys.active).to.be.ok;
    });

    after(async () => {
      await deleteRealm(kcAdminClient, currentRealmName);
    });
  });

  describe("Realm Session Management", () => {
    before(async () => {
      kcAdminClient = new KeycloakAdminClient();
      await kcAdminClient.auth(credentials);

      const created = await createRealm(kcAdminClient);
      currentRealmId = created.realmId;
      currentRealmName = created.realmName;
    });

    it("gets client session stats", async () => {
      const sessionStats = await kcAdminClient.realms.getClientSessionStats({
        realm: currentRealmName,
      });
      expect(sessionStats).to.be.ok;
    });

    it("push revocation", async () => {
      const push = await kcAdminClient.realms.pushRevocation({
        realm: currentRealmName,
      });
      expect(push).to.be.ok;
    });

    it("logs out all sessions", async () => {
      const logout = await kcAdminClient.realms.logoutAll({
        realm: currentRealmName,
      });
      expect(logout).to.be.ok;
    });

    after(async () => {
      await deleteRealm(kcAdminClient, currentRealmName);
    });
  });

  describe("Realm connection settings", () => {
    it("should fail with invalid ldap settings", async () => {
      try {
        await kcAdminClient.realms.testLDAPConnection(
          { realm: "master" },
          {
            action: "testConnection",
            authType: "simple",
            bindCredential: "1",
            bindDn: "1",
            connectionTimeout: "",
            connectionUrl: "1",
            startTls: "",
            useTruststoreSpi: "always",
          },
        );
        fail("exception should have been thrown");
      } catch (error) {
        expect(error).to.be.ok;
      }
    });

    it("should fail with invalid smtp settings", async () => {
      try {
        const user = (
          await kcAdminClient.users.find({ username: credentials.username })
        )[0];
        user.email = "test@test.com";
        await kcAdminClient.users.update({ id: user.id! }, user);
        await kcAdminClient.realms.testSMTPConnection(
          { realm: "master" },
          {
            from: "cdd1641ff4-1781a4@inbox.mailtrap.io",
            host: "localhost",
            port: 3025,
          },
        );
        fail("exception should have been thrown");
      } catch (error) {
        expect(error).to.be.ok;
      }
    });

    it("should fail with invalid ldap server capabilities", async () => {
      try {
        await kcAdminClient.realms.ldapServerCapabilities(
          { realm: "master" },
          {
            action: "testConnection",
            authType: "simple",
            bindCredential: "1",
            bindDn: "1",
            connectionTimeout: "",
            connectionUrl: "1",
            startTls: "",
            useTruststoreSpi: "always",
          },
        );
        fail("exception should have been thrown");
      } catch (error) {
        expect(error).to.be.ok;
      }
    });
  });

  describe("Realm localization", () => {
    currentRealmName = "master";

    it.skip("enable localization", async () => {
      await kcAdminClient.realms.getRealmLocalizationTexts({
        realm: currentRealmName,
        selectedLocale: "nl",
      });
    });

    it.skip("should add localization", async () => {
      await kcAdminClient.realms.addLocalization(
        { realm: currentRealmName, selectedLocale: "nl", key: "theKey" },
        "value",
      );
    });

    it.skip("should get realm specific locales", async () => {
      const locales = await kcAdminClient.realms.getRealmSpecificLocales({
        realm: currentRealmName,
      });

      expect(locales).to.be.ok;
      expect(locales).to.be.deep.eq(["nl"]);
    });

    it.skip("should get localization for specified locale", async () => {
      const texts = await kcAdminClient.realms.getRealmLocalizationTexts({
        realm: currentRealmName,
        selectedLocale: "nl",
      });

      expect(texts).to.be.ok;
      expect(texts.theKey).to.be.eq("value");
    });

    it.skip("should delete localization for specified locale key", async () => {
      await kcAdminClient.realms.deleteRealmLocalizationTexts({
        realm: currentRealmName,
        selectedLocale: "nl",
        key: "theKey",
      });

      const texts = await kcAdminClient.realms.getRealmLocalizationTexts({
        realm: currentRealmName,
        selectedLocale: "nl",
      });
      expect(texts).to.be.ok;
      expect(texts).to.be.deep.eq({});
    });

    it.skip("should delete localization for specified locale", async () => {
      await kcAdminClient.realms.deleteRealmLocalizationTexts({
        realm: currentRealmName,
        selectedLocale: "nl",
      });

      const locales = await kcAdminClient.realms.getRealmSpecificLocales({
        realm: currentRealmName,
      });
      expect(locales).to.be.ok;
      expect(locales).to.be.deep.eq([]);
    });
  });
});
