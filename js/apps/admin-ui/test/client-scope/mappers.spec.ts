import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import { test } from "@playwright/test";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";
import { createTestBed } from "../support/testbed.ts";
import { clickCancelButton, clickSaveButton } from "../utils/form.ts";
import { login, navigateTo } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import {
  assertRowExists,
  clickTableRowItem,
  searchItem,
} from "../utils/table.ts";
import {
  addMappersByConfiguration,
  addPredefinedMappers,
  assertMapperConfigurationValues,
  ClaimJsonType,
  fillMapperDetails,
  goToMappersTab,
  removeMappers,
} from "./mappers.ts";

test.describe("Mappers tab", () => {
  const placeHolderClientScope = "Search for client scope";
  const testBedData: RealmRepresentation = {
    clientScopes: [
      {
        name: "test-scope",
        protocol: "openid-connect",
        protocolMappers: [
          {
            name: "test-mapper",
            protocol: "openid-connect",
            protocolMapper: "oidc-hardcoded-claim-mapper",
          },
        ],
      },
    ],
  };

  test("updates a predefined mapper", async ({ page }) => {
    const placeHolder = "Search for mapper";
    const mappers = ["birthdate", "email", "family name"];

    await using testBed = await createTestBed(testBedData);

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await searchItem(page, placeHolderClientScope, "test-scope");
    await clickTableRowItem(page, "test-scope");
    await goToMappersTab(page);

    await addPredefinedMappers(page, mappers);

    // Configure first mapper
    await searchItem(page, placeHolder, mappers[0]);
    await clickTableRowItem(page, mappers[0]);
    await fillMapperDetails(
      page,
      mappers[0] + "1",
      mappers[0] + "2",
      ClaimJsonType.Long,
    );

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Mapping successfully updated");

    await navigateTo(page, toClientScopes({ realm: testBed.realm }));
    await searchItem(page, placeHolderClientScope, "test-scope");
    await clickTableRowItem(page, "test-scope");
    await goToMappersTab(page);
    await searchItem(page, placeHolder, mappers[0]);
    await clickTableRowItem(page, mappers[0]);

    await assertMapperConfigurationValues(
      page,
      mappers[0] + "1",
      mappers[0] + "2",
      ClaimJsonType.Long,
    );

    await clickCancelButton(page);
  });

  test("creates and removes mappers by configuration", async ({ page }) => {
    const mapperNames = ["Pairwise subject identifier", "Allowed Web Origins"];

    await using testBed = await createTestBed(testBedData);

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await searchItem(page, placeHolderClientScope, "test-scope");
    await clickTableRowItem(page, "test-scope");
    await goToMappersTab(page);

    await addMappersByConfiguration(page, mapperNames);

    await assertRowExists(page, mapperNames[0]);

    await removeMappers(page, mapperNames);
  });

  test("auto-navigates after creating an Audience mapper", async ({ page }) => {
    const mapperName = "test-audience-mapper";

    await using testBed = await createTestBed(testBedData);

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await searchItem(page, placeHolderClientScope, "test-scope");
    await clickTableRowItem(page, "test-scope");
    await goToMappersTab(page);

    await page.getByRole("button", { name: "Add mapper" }).click();
    await page.getByRole("menuitem", { name: "By configuration" }).click();

    await page
      .locator('[role="dialog"]')
      .getByText("Audience", { exact: true })
      .click();
    await page.getByTestId("name").fill(mapperName);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Mapping successfully created");

    await assertRowExists(page, mapperName);
    await page
      .getByRole("button", { name: "Add mapper" })
      .waitFor({ state: "visible" });

    await removeMappers(page, [mapperName]);
  });
});
