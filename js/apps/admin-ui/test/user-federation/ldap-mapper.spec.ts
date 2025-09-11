import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { selectItem } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToUserFederation } from "../utils/sidebar.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  searchItem,
} from "../utils/table.ts";
import {
  clickAddMapper,
  fillHardwareAttributeMapper,
  goToMapperTab,
  saveMapper,
} from "./ldap-mapper.ts";

const provider = "ldap";

const ldapName = "ldap-mappers-testing";

// connection and authentication settings
const mapperCreatedSuccess = "Mapping successfully created";
const mapperDeletedSuccess = "Mapping successfully deleted";
const groupName = "aa-uf-mappers-group";
const clientName = "aa-uf-mappers-client";
const roleName = "aa-uf-mappers-role";

// mapperType variables
const hcAttMapper = "hardcoded-attribute-mapper";
const hcLdapGroupMapper = "hardcoded-ldap-group-mapper";
const hcLdapAttMapper = "hardcoded-ldap-attribute-mapper";
const roleLdapMapper = "role-ldap-mapper";
const hcLdapRoleMapper = "hardcoded-ldap-role-mapper";

test.describe.serial("User Fed LDAP mapper tests", () => {
  const realmName = `user-federation-kerberos_${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createUserFederation(realmName, {
      providerId: provider,
      name: ldapName,
      config: {
        connectionUrl: ["ldap://localhost:3004"],
        bindType: ["simple"],
        bindDn: ["cn=user,dc=test"],
        bindCredential: ["user"],
        usersDn: ["user-dn-1"],
        rdnLDAPAttribute: ["uid"],
        userObjectClasses: ["inetOrgPerson, organizationalPerson"],
        editMode: ["READ_ONLY"],
      },
    });
    await adminClient.createGroup(groupName, realmName);
    await adminClient.createClient({
      realm: realmName,
      clientId: clientName,
    });
    await adminClient.createRealmRole({
      realm: realmName,
      name: roleName,
    });
  });
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToUserFederation(page);
    await page.getByRole("link", { name: ldapName }).click();
    await goToMapperTab(page);
  });

  test("Delete default mappers", async ({ page }) => {
    const creationDateMapper = "creation date";
    const emailMapper = "email";
    const lastNameMapper = "last name";
    const modifyDateMapper = "modify date";

    await searchItem(page, "Search", creationDateMapper);
    await clickRowKebabItem(page, creationDateMapper, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, mapperDeletedSuccess);
    await assertRowExists(page, creationDateMapper, false);

    await searchItem(page, "Search", emailMapper);
    await clickRowKebabItem(page, emailMapper, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, mapperDeletedSuccess);
    await assertRowExists(page, emailMapper, false);

    await searchItem(page, "Search", lastNameMapper);
    await clickRowKebabItem(page, lastNameMapper, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, mapperDeletedSuccess);
    await assertRowExists(page, lastNameMapper, false);

    await searchItem(page, "Search", modifyDateMapper);
    await clickRowKebabItem(page, modifyDateMapper, "Delete");
    await confirmModal(page);
    await assertNotificationMessage(page, mapperDeletedSuccess);
    await assertRowExists(page, modifyDateMapper, false);
  });

  test("Create hardcoded attribute mapper", async ({ page }) => {
    await clickAddMapper(page);
    await page.getByTestId("name").fill(`${hcAttMapper}-test`);
    await selectItem(page, "#kc-providerId", hcAttMapper);
    await fillHardwareAttributeMapper(page, {
      config: {
        "user.model.attribute": ["middleName"],
        "attribute.value": ["test"],
      },
    });
    await saveMapper(page);
    await assertNotificationMessage(page, mapperCreatedSuccess);
    await assertRowExists(page, hcAttMapper, true);
  });

  test("Create hardcoded ldap group mapper", async ({ page }) => {
    await clickAddMapper(page);
    await selectItem(page, "#kc-providerId", hcLdapGroupMapper);

    await fillHardwareAttributeMapper(page, {
      name: `${hcLdapGroupMapper}-test`,
      config: {
        group: [groupName],
      },
    });
    await saveMapper(page);
    await assertNotificationMessage(page, mapperCreatedSuccess);
    await assertRowExists(page, hcLdapGroupMapper, true);
  });

  test("Create hardcoded ldap attribute mapper", async ({ page }) => {
    await clickAddMapper(page);
    await selectItem(page, "#kc-providerId", hcLdapAttMapper);
    await fillHardwareAttributeMapper(page, {
      name: `${hcLdapAttMapper}-test`,
      config: {
        "ldap.attribute.name": ["someName"],
        "ldap.attribute.value": ["test"],
      },
    });
    await saveMapper(page);
    await assertNotificationMessage(page, mapperCreatedSuccess);
    await assertRowExists(page, hcLdapAttMapper, true);
  });

  test("Create hardcoded ldap role mapper", async ({ page }) => {
    await clickAddMapper(page);
    await selectItem(page, "#kc-providerId", hcLdapRoleMapper);
    await fillHardwareAttributeMapper(page, {
      name: `${hcLdapRoleMapper}-test`,
      config: {
        role: [roleName],
      },
    });
    await saveMapper(page);
    await assertNotificationMessage(page, mapperCreatedSuccess);
    await assertRowExists(page, hcLdapRoleMapper, true);
  });

  test("Create role ldap mapper", async ({ page }) => {
    await clickAddMapper(page);
    await selectItem(page, "#kc-providerId", roleLdapMapper);
    await fillHardwareAttributeMapper(page, {
      name: `${roleLdapMapper}-test`,
      config: {
        roleDn: ["role-dn"],
      },
    });
    await saveMapper(page);
    await assertNotificationMessage(page, mapperCreatedSuccess);
    await assertRowExists(page, roleLdapMapper, true);
  });
});
