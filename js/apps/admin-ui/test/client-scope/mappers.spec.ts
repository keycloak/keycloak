import { test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import { clickCancelButton, clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
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
import adminClient from "../utils/AdminClient.ts";

test.describe.serial("Mappers tab test", () => {
  const placeHolderClientScope = "Search for client scope";
  const placeHolder = "Search for mapper";

  const scopeName = `client-scope-mapper-${uuidv4()}`;

  test.beforeAll(async () => {
    const { id } = await adminClient.createClientScope({
      name: scopeName,
      description: "",
      protocol: "openid-connect",
    });
    await adminClient.addMapping(id, {
      name: "test",
      protocol: "openid-connect",
      protocolMapper: "oidc-hardcoded-claim-mapper",
    });
  });

  test.afterAll(() => adminClient.deleteClientScope(scopeName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToClientScopes(page);
    await searchItem(page, placeHolderClientScope, scopeName);
    await clickTableRowItem(page, scopeName);
    await goToMappersTab(page);
  });

  test("CRUD predefined mappers", async ({ page }) => {
    const mappers = ["birthdate", "email", "family name"];

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

    await goToClientScopes(page);
    await searchItem(page, placeHolderClientScope, scopeName);
    await clickTableRowItem(page, scopeName);
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

  test("crud mappers by configuration", async ({ page }) => {
    const mapperNames = ["Pairwise subject identifier", "Allowed Web Origins"];
    await addMappersByConfiguration(page, mapperNames);

    await assertRowExists(page, mapperNames[0]);

    await removeMappers(page, mapperNames);
  });
});
