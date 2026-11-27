import { type Page, expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import {
  assertSelectValue,
  selectItem,
  selectMultiItem,
} from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { clickTableRowItem } from "../utils/table.ts";
import {
  addMapper,
  clickSaveButton,
  clickSaveMapper,
  createDefaultTrustProvider,
  goToMappersTab,
} from "./main.ts";

const realmName = `idp-oid4vp-${uuid()}`;
const alias = "oid4vp";
const trustAlias = "issuer-trust";
const credentials = "config.requestedCredentials.credentials";
const credentialSets = "config.requestedCredentials.credentialSets";

const byId = (page: Page, id: string) => page.locator(`[id="${id}"]`);

async function fillCredential(
  page: Page,
  index: number,
  id: string,
  type: string,
) {
  await page.getByTestId(`${credentials}.${index}.id`).fill(id);
  await page.getByTestId(`${credentials}.${index}.types0`).fill(type);
  await byId(page, `${credentials}.${index}.trustMaterialIdps`).click();
  await page.getByRole("option", { name: trustAlias, exact: true }).click();
}

async function storedRequestedCredentials() {
  const idp = await adminClient.getIdentityProvider(alias, realmName);
  return JSON.parse(idp!.config!.requestedCredentials);
}

test.describe.serial("OpenID4VP identity provider test", () => {
  test.beforeAll(() => adminClient.createRealm(realmName));

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page, { to: { pathname: `/${realmName}/identity-providers` } });
  });

  test("should create a provider requesting one credential", async ({
    page,
  }) => {
    await createDefaultTrustProvider(
      page,
      trustAlias,
      `${SERVER_URL}/realms/master/protocol/openid-connect/certs`,
    );
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oid4vp/add`,
    );

    await fillCredential(page, 0, "pid", "urn:eudi:pid:1");
    await selectItem(
      page,
      byId(page, "config.requestedCredentials.principal.credential"),
      "pid",
    );
    await page
      .getByTestId("config.requestedCredentials.principal.claim")
      .fill("personal_administrative_number");
    await page.getByTestId("createProvider").click();

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
    expect(await storedRequestedCredentials()).toEqual({
      credentials: [
        {
          id: "pid",
          format: "dc+sd-jwt",
          types: ["urn:eudi:pid:1"],
          trustMaterialIdps: [trustAlias],
        },
      ],
      principal: {
        credential: "pid",
        claim: "personal_administrative_number",
      },
    });
  });

  test("should save credential sets with alternative options", async ({
    page,
  }) => {
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oid4vp/${alias}/settings`,
    );

    await page.getByTestId("addCredential").click();
    await fillCredential(page, 1, "employee", "urn:example:employee:1");
    await page.getByTestId("addCredentialSet").click();
    await selectMultiItem(
      page,
      byId(page, `${credentialSets}.0.options.0.credentials`),
      "pid",
      "employee",
    );
    await page.getByTestId("addCredentialSetOption0").click();
    await selectMultiItem(
      page,
      byId(page, `${credentialSets}.0.options.1.credentials`),
      "pid",
    );
    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
    const requested = await storedRequestedCredentials();
    expect(requested.credentials).toHaveLength(2);
    expect(requested.credentialSets).toEqual([
      { options: [["pid", "employee"], ["pid"]] },
    ]);
  });

  test("should select the credential of a mapper from the requested credentials", async ({
    page,
  }) => {
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oid4vp/${alias}/settings`,
    );
    await goToMappersTab(page);
    await addMapper(page, "oid4vp-sd-jwt-user-attribute", "employee-mapper");

    await assertSelectValue(byId(page, "credential"), "pid");
    await selectItem(page, byId(page, "credential"), "employee");
    await page.getByTestId("claim").fill("employee_number");
    await clickSaveMapper(page);

    await clickTableRowItem(page, "employee-mapper");
    await assertSelectValue(byId(page, "credential"), "employee");
  });

  test("should require the principal credential in every option of a required set", async ({
    page,
  }) => {
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oid4vp/${alias}/settings`,
    );

    await page.getByTestId("addCredentialSetOption0").click();
    await selectMultiItem(
      page,
      byId(page, `${credentialSets}.0.options.2.credentials`),
      "employee",
    );
    await clickSaveButton(page);

    await assertNotificationMessage(
      page,
      "Could not update the provider. The principal credential 'pid' must be part of every option of at least one required credential set",
    );
    expect((await storedRequestedCredentials()).credentialSets).toEqual([
      { options: [["pid", "employee"], ["pid"]] },
    ]);
  });

  test("should create a provider after removing an earlier credential", async ({
    page,
  }) => {
    await page.goto(
      `${SERVER_URL}/admin/master/console/#/${realmName}/identity-providers/oid4vp/add`,
    );
    await page.getByTestId("alias").fill("oid4vp-removed");

    await fillCredential(page, 0, "draft", "urn:example:draft:1");
    await page.getByTestId("addCredential").click();
    await fillCredential(page, 1, "pid", "urn:eudi:pid:1");
    await page.getByTestId("removeCredential0").click();
    await selectItem(
      page,
      byId(page, "config.requestedCredentials.principal.credential"),
      "pid",
    );
    await page
      .getByTestId("config.requestedCredentials.principal.claim")
      .fill("personal_administrative_number");
    await page.getByTestId("createProvider").click();

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
  });
});
