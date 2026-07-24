import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import {
  addAuthConstraints,
  addMapper,
  clickCancelMapper,
  clickSaveMapper,
  createSAMLProvider,
  goToMappersTab,
} from "./main.ts";
import { editSAMLSettings } from "./saml.ts";

test.describe.serial("SAML identity provider test", () => {
  const samlProviderName = "saml";
  const samlDisplayName = "saml";

  test.afterAll(() => adminClient.deleteIdentityProvider(samlProviderName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToIdentityProviders(page);
  });

  test("should create a SAML provider using entity descriptor", async ({
    page,
  }) => {
    await createSAMLProvider(page, samlProviderName, samlDisplayName);
    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );
  });
});

test.describe.serial("SAML identity provider test", () => {
  const samlProviderName = "SAML v2.0";
  const classRefName = "acClassRef-1";
  const declRefName = "acDeclRef-1";
  const alias = `edit-oidc-${uuid()}`;

  test.beforeEach(async ({ page }) => {
    await adminClient.createIdentityProvider(samlProviderName, alias);
    await login(page);
    await goToIdentityProviders(page);
    await clickTableRowItem(page, samlProviderName);
  });

  test.afterEach(() => adminClient.deleteIdentityProvider(alias));

  test("should add auth constraints to existing SAML provider", async ({
    page,
  }) => {
    await addAuthConstraints(page, classRefName, declRefName);
    await assertNotificationMessage(page, "Provider successfully updated");
  });

  const mapperTests = [
    { type: "saml-advanced-role", name: "SAML mapper" },
    {
      type: "saml-username",
      name: "SAML Username Template Importer Mapper",
    },
    {
      type: "hardcoded-user-session-attribute",
      name: "Hardcoded User Session Attribute",
    },
    { type: "saml-user-attribute", name: "Attribute Importer" },
    { type: "oidc-hardcoded-role", name: "Hardcoded Role" },
    { type: "saml-role", name: "SAML Attribute To Role" },
  ];

  for (const { type, name } of mapperTests) {
    test(`should add SAML mapper of type ${name}`, async ({ page }) => {
      await goToMappersTab(page);
      await addMapper(page, type, name);
      await clickSaveMapper(page);
      await assertNotificationMessage(page, "Mapper created successfully.");
      await clickCancelMapper(page);
    });
  }

  test("should edit SAML settings", async ({ page }) => {
    await editSAMLSettings(page, samlProviderName);
    await assertNotificationMessage(page, "Provider successfully updated");
  });
});
