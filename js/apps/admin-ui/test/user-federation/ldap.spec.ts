import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToRealm, goToUserFederation } from "../utils/sidebar.ts";
import { clickAddProvider, clickSave } from "./kerberos.ts";
import {
  assertEvictionValues,
  clickLdapCard,
  fillEviction,
  fillLdapForm,
  selectEvictionPolicy,
} from "./ldap.ts";
import { switchToggle } from "../utils/form.ts";

const provider = "ldap";

const firstLdapName = "my-ldap";
const firstLdapVendor = "Active Directory";
const updatedLdapName = `${firstLdapName}-updated`;

// connection and authentication settings
const connectionUrlValid = "ldap://localhost:3004";
const bindTypeSimple = "simple";
const truststoreSpiAlways = "Always";
const connectionTimeoutTwoSecs = "2000";
const bindDnCnDc = "cn=user,dc=test";
const bindCredsValid = "user";

const bindTypeNone = "none";
const truststoreSpiNever = "Never";

// ldap searching and updating
const editModeReadOnly = "READ_ONLY";
const editModeWritable = "WRITABLE";

const firstUsersDn = "user-dn-1";
const firstUserLdapAtt = "uid";
const firstRdnLdapAtt = "uid";
const firstUuidLdapAtt = "entryUUID";
const firstUserObjClasses = "inetOrgPerson, organizationalPerson";
const firstUserLdapFilter = "(first-filter)";
const firstReadTimeout = "5000";

const secondUsersDn = "user-dn-2";
const secondUserLdapAtt = "cn";
const secondRdnLdapAtt = "cn";
const secondUuidLdapAtt = "objectGUID";
const secondUserObjClasses = "person, organizationalPerson, user";

const weeklyPolicy = "EVICT_WEEKLY";
const dailyPolicy = "EVICT_DAILY";
const newLdapDay = "Wednesday";
const newLdapHour = "15";
const newLdapMinute = "55";

const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const validatePasswordPolicyFailMessage =
  "User federation provider could not be saved: Validate Password Policy is applicable only with WRITABLE edit mode";

test.describe.serial("User Federation LDAP tests", () => {
  const realmName = `user-federation-ldap-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToUserFederation(page);
  });

  test("Should create LDAP provider from empty state", async ({ page }) => {
    await clickAddProvider(page, provider);

    await fillLdapForm(page, {
      name: "new-ldap",
      config: {
        vendor: [firstLdapVendor],
        connectionUrl: [connectionUrlValid],
        bindType: [bindTypeSimple],
        useTruststoreSpi: [truststoreSpiAlways],
        connectionTimeout: [connectionTimeoutTwoSecs],
        bindDn: [bindDnCnDc],
        bindCredential: [bindCredsValid],
        editMode: [editModeReadOnly],
        usersDn: [firstUsersDn],
        usernameLDAPAttribute: [firstUserLdapAtt],
        rdnLDAPAttribute: [firstRdnLdapAtt],
        uuidLDAPAttribute: [firstUuidLdapAtt],
        userObjectClasses: [firstUserObjClasses],
        customUserSearchFilter: [firstUserLdapFilter],
        readTimeout: [firstReadTimeout],
      },
    });
    await clickSave(page, provider);

    await assertNotificationMessage(page, createdSuccessMessage);
    await goToUserFederation(page);
  });

  test.describe.serial("Edit provider", () => {
    test.beforeAll(() =>
      adminClient.createUserFederation(realmName, {
        providerId: provider,
        name: firstLdapName,
        config: {
          vendor: [firstLdapVendor],
          connectionUrl: [connectionUrlValid],
          bindType: [bindTypeSimple],
          bindDn: [bindDnCnDc],
          bindCredential: [bindCredsValid],
          editMode: [editModeReadOnly],
          usersDn: [firstUsersDn],
          usernameLDAPAttribute: [firstUserLdapAtt],
          rdnLDAPAttribute: [firstRdnLdapAtt],
        },
      }),
    );

    test.beforeEach(async ({ page }) => {
      await clickLdapCard(page, firstLdapName);
    });

    test("Should fail updating advanced settings", async ({ page }) => {
      await switchToggle(page, page.getByTestId("ldapv3-password"));
      await switchToggle(page, page.getByTestId("password-policy"));
      await switchToggle(page, page.getByTestId("trust-email"));
      await clickSave(page, provider);
      await assertNotificationMessage(page, validatePasswordPolicyFailMessage);
    });

    test("Should update advanced settings", async ({ page }) => {
      await switchToggle(page, page.getByTestId("ldapv3-password"));
      await switchToggle(page, page.getByTestId("password-policy"));
      await switchToggle(page, page.getByTestId("trust-email"));

      await fillLdapForm(page, {
        name: updatedLdapName,
        config: {
          connectionUrl: [connectionUrlValid],
          editMode: [editModeWritable],
          usersDn: [secondUsersDn],
          usernameLDAPAttribute: [secondUserLdapAtt],
          rdnLDAPAttribute: [secondRdnLdapAtt],
          uuidLDAPAttribute: [secondUuidLdapAtt],
          userObjectClasses: [secondUserObjClasses],
        },
      });
      await clickSave(page, provider);
      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickLdapCard(page, updatedLdapName);
      await expect(page.getByTestId("ldapv3-password")).toBeChecked();
      await expect(page.getByTestId("password-policy")).toBeChecked();
      await expect(page.getByTestId("trust-email")).toBeChecked();
    });

    test("Should set cache policy to evict_daily", async ({ page }) => {
      await selectEvictionPolicy(page, dailyPolicy);
      await fillEviction(page, ["hour", newLdapHour]);
      await fillEviction(page, ["minute", newLdapMinute]);
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickLdapCard(page, firstLdapName);

      await assertEvictionValues(page, [
        ["hour", newLdapHour],
        ["minute", newLdapMinute],
      ]);
    });

    test("Should set cache policy to evict_weekly", async ({ page }) => {
      await selectEvictionPolicy(page, weeklyPolicy);
      await fillEviction(page, ["day", newLdapDay]);
      await fillEviction(page, ["hour", newLdapHour]);
      await fillEviction(page, ["minute", newLdapMinute]);
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickLdapCard(page, firstLdapName);

      await assertEvictionValues(page, [
        ["day", newLdapDay],
        ["hour", newLdapHour],
        ["minute", newLdapMinute],
      ]);
    });

    test("Update connection and authentication settings and save", async ({
      page,
    }) => {
      await fillLdapForm(page, {
        name: updatedLdapName,
        config: {
          connectionUrl: [connectionUrlValid],
          bindType: [bindTypeNone],
          useTruststoreSpi: [truststoreSpiNever],
          connectionTimeout: [connectionTimeoutTwoSecs],
        },
      });
      await clickSave(page, provider);
      await assertNotificationMessage(page, savedSuccessMessage);
    });
  });
});
