import { type Page, expect } from "@playwright/test";
import {
  assertSelectValue,
  clickSaveButton,
  selectItem,
} from "../utils/form.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { confirmModal } from "../utils/modal.ts";
import {
  assertRowExists,
  clickRowKebabItem,
  clickSelectRow,
} from "../utils/table.ts";

export const ClaimJsonType = {
  String: "String",
  Long: "long",
  Int: "int",
  Boolean: "boolean",
  Json: "JSON",
};

export async function goToMappersTab(page: Page) {
  await page.getByTestId("mappers").click();
}

async function clickAddMapperButton(page: Page) {
  await page.getByRole("button", { name: "Add mapper" }).click();
}

export async function addPredefinedMappers(page: Page, mappers: string[]) {
  await clickAddMapperButton(page);
  await page.getByRole("menuitem", { name: "From predefined mappers" }).click();

  for (const mapperName of mappers) {
    await clickSelectRow(page, "Add predefined mappers", mapperName);
  }

  await confirmModal(page);
  await assertNotificationMessage(page, "Mapping successfully created");

  for (const mapperName of mappers) {
    await assertRowExists(page, mapperName);
  }
}

export async function addMappersByConfiguration(page: Page, mappers: string[]) {
  for (const mapperName of mappers) {
    await clickAddMapperButton(page);
    await page.getByRole("menuitem", { name: "By configuration" }).click();

    await page.getByText(mapperName, { exact: true }).click();
    await page.getByTestId("name").fill(mapperName);
    await clickSaveButton(page);
    await assertNotificationMessage(page, "Mapping successfully created");
  }
}

function getUserAttribute(page: Page) {
  return page.getByTestId("config.user🍺attribute");
}

function getClaimName(page: Page) {
  return page.getByTestId("claim.name");
}

function getClaimJsonType(page: Page) {
  return page.getByLabel("Claim JSON Type");
}

export async function fillMapperDetails(
  page: Page,
  userAttribute: string,
  tokenClaimName: string,
  jsonType: string,
) {
  await getUserAttribute(page).fill(userAttribute);
  await getClaimName(page).fill(tokenClaimName);
  await selectItem(page, getClaimJsonType(page), jsonType);
}

export async function assertMapperConfigurationValues(
  page: Page,
  userAttribute: string,
  tokenClaimName: string,
  jsonType: string,
) {
  await expect(getUserAttribute(page)).toHaveValue(userAttribute);
  await expect(getClaimName(page)).toHaveValue(tokenClaimName);
  await assertSelectValue(getClaimJsonType(page), jsonType);
}

export async function removeMappers(page: Page, mapperNames: string[]) {
  for (const name of mapperNames) {
    await clickRowKebabItem(page, name, "Delete");
    await assertNotificationMessage(page, "Mapping successfully deleted");
  }
}
