import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { selectItem, switchOff, switchToggle } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import { goToRealm, goToUserFederation } from "../utils/sidebar.ts";
import {
  clickAddProvider,
  clickSave,
  clickUserFederationCard,
  fillKerberosForm,
} from "./kerberos.ts";

const provider = "kerberos";
const initCapProvider = provider.charAt(0).toUpperCase() + provider.slice(1);

const kerberosName = "my-kerberos";
const kerberosRealm = "my-realm";
const kerberosPrincipal = "my-principal";
const kerberosKeytab = "my-keytab";

const firstKerberosName = `${kerberosName}-1`;
const firstKerberosRealm = `${kerberosRealm}-1`;
const firstKerberosPrincipal = `${kerberosPrincipal}-1`;
const firstKerberosKeytab = `${kerberosKeytab}-1`;

const secondKerberosName = `${kerberosName}-2`;
const secondKerberosRealm = `${kerberosRealm}-2`;
const secondKerberosPrincipal = `${kerberosPrincipal}-2`;
const secondKerberosKeytab = `${kerberosKeytab}-2`;

const defaultPolicy = "DEFAULT";
const weeklyPolicy = "EVICT_WEEKLY";
const dailyPolicy = "EVICT_DAILY";
const lifespanPolicy = "MAX_LIFESPAN";
const noCachePolicy = "NO_CACHE";

const newKerberosDay = "Wednesday";
const newKerberosHour = "15";
const newKerberosMinute = "55";
const maxLifespan = 5;

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";

test.describe.serial("User Fed Kerberos tests", () => {
  const realmName = `user-federation-kerberos_${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
  });

  test("Should create Kerberos provider from empty state", async ({ page }) => {
    await goToUserFederation(page);
    await clickAddProvider(page, provider);

    await fillKerberosForm(page, {
      alias: "new-kerberos",
      config: {
        kerberosRealm: firstKerberosRealm,
        serverPrincipal: firstKerberosPrincipal,
        keyTab: firstKerberosKeytab,
      },
    });
    await clickSave(page, provider);
    await assertNotificationMessage(page, createdSuccessMessage);
  });

  test.describe.serial("Edit Kerberos provider", () => {
    test.beforeAll(() =>
      adminClient.createUserFederation(realmName, {
        providerId: provider,
        name: firstKerberosName,
        config: {
          kerberosRealm: [firstKerberosRealm],
          serverPrincipal: [firstKerberosPrincipal],
          keyTab: [firstKerberosKeytab],
        },
      }),
    );

    test.beforeEach(async ({ page }) => {
      await goToUserFederation(page);
    });

    test("Should edit existing Kerberos provider and cancel", async ({
      page,
    }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await selectItem(page, "#kc-cache-policy", weeklyPolicy);
      await selectItem(page, "#kc-eviction-day", newKerberosDay);
      await selectItem(page, "#kc-eviction-hour", newKerberosHour);
      await selectItem(page, "#kc-eviction-minute", newKerberosMinute);
      await page.getByTestId(`${provider}-cancel`).click();

      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.locator("#kc-cache-policy")).toHaveText(defaultPolicy);
    });

    test("Should edit existing Kerberos provider", async ({ page }) => {
      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);
      await expect(
        page.getByRole("heading", { name: "Required Settings" }),
      ).toBeVisible();

      await switchToggle(page, page.getByTestId("debug"));
      await switchToggle(
        page,
        page.getByTestId("allow-password-authentication"),
      );
      await switchToggle(page, page.getByTestId("update-first-login"));

      await clickSave(page, provider);
      await assertNotificationMessage(page, savedSuccessMessage);

      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.getByTestId("debug")).toBeChecked();
      await expect(
        page.getByTestId("allow-password-authentication"),
      ).toBeChecked();
      await expect(page.getByTestId("update-first-login")).toBeChecked();
    });

    test("Should set cache policy to evict_daily", async ({ page }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await selectItem(page, "#kc-cache-policy", dailyPolicy);
      await selectItem(page, "#kc-eviction-hour", newKerberosHour);
      await selectItem(page, "#kc-eviction-minute", newKerberosMinute);
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.getByText(dailyPolicy)).toBeVisible();
      await expect(page.getByText(defaultPolicy)).toBeHidden();
    });

    test("Should set cache policy to evict_weekly", async ({ page }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await selectItem(page, "#kc-cache-policy", weeklyPolicy);
      await selectItem(page, "#kc-eviction-day", newKerberosDay);
      await selectItem(page, "#kc-eviction-hour", newKerberosHour);
      await selectItem(page, "#kc-eviction-minute", newKerberosMinute);
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.getByText(weeklyPolicy)).toBeVisible();
      await expect(page.getByText(defaultPolicy)).toBeHidden();
    });

    test("Should set cache policy to max_lifespan", async ({ page }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await selectItem(page, "#kc-cache-policy", lifespanPolicy);
      for (let i = 0; i < maxLifespan; i++) {
        await page.getByTestId("kerberos-cache-lifespan").click();
      }
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.getByText(lifespanPolicy)).toBeVisible();
      await expect(page.getByText(defaultPolicy)).toBeHidden();
    });

    test("Should set cache policy to no_cache", async ({ page }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await selectItem(page, "#kc-cache-policy", noCachePolicy);
      await clickSave(page, provider);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);
      await clickUserFederationCard(page, firstKerberosName);

      await expect(page.getByText(noCachePolicy)).toBeVisible();
      await expect(page.getByText(defaultPolicy)).toBeHidden();
    });

    test("Should disable an existing Kerberos provider", async ({ page }) => {
      await clickUserFederationCard(page, firstKerberosName);
      await switchOff(page, "#Kerberos-switch");
      await confirmModal(page);

      await assertNotificationMessage(page, savedSuccessMessage);
      await goToUserFederation(page);

      await expect(page.locator("text=Disabled")).toBeVisible();
    });

    test("Should create new Kerberos provider using the New Provider dropdown", async ({
      page,
    }) => {
      await page.getByText(addProviderMenu).click();
      await page.getByRole("menuitem", { name: initCapProvider }).click();

      await fillKerberosForm(page, {
        alias: secondKerberosName,
        config: {
          kerberosRealm: secondKerberosRealm,
          serverPrincipal: secondKerberosPrincipal,
          keyTab: secondKerberosKeytab,
        },
      });
      await clickSave(page, provider);

      await assertNotificationMessage(page, createdSuccessMessage);
      await expect(page.getByText(secondKerberosName)).toBeVisible();
    });
  });
});
