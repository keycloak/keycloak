import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";
import { clickTableToolbarItem } from "../utils/table.ts";

export async function goToClientScopesTab(page: Page) {
  await page.getByTestId("clientScopesTab").click();
}

export async function goToClientScopeEvaluateTab(page: Page) {
  await page.getByTestId("clientScopesEvaluateTab").click();
}

export async function clickAddClientScope(page: Page) {
  const toolbar = page.getByTestId("table-toolbar");
  const addClientScopeAction = toolbar
    .getByRole("button", { name: /^Add client scope$/i })
    .or(toolbar.getByRole("link", { name: /^Add client scope$/i }))
    .first();
  const addScopeAction = toolbar
    .getByRole("button", { name: /^Add scope$/i })
    .or(toolbar.getByRole("link", { name: /^Add scope$/i }))
    .first();

  await expect(addClientScopeAction.or(addScopeAction).first()).toBeVisible({
    timeout: 5_000,
  });

  if (await addClientScopeAction.isVisible()) {
    await clickTableToolbarItem(page, "Add client scope");
    return;
  }

  await clickTableToolbarItem(page, "Add scope");
}

export async function clickAddScope(page: Page, option: string) {
  await page.getByTestId("add-dropdown").click();
  await page.getByRole("menuitem", { name: option }).click();
}

export async function assertTableCellDropdownValue(page: Page, value: string) {
  await expect(page.getByTestId("cell-dropdown")).toHaveText(value);
}

export async function goToGenerateAccessTokenTab(page: Page) {
  await page.getByTestId("generated-access-token-tab").click();
}

export async function assertHasAccessTokenGenerated(
  page: Page,
  username: string,
) {
  await goToGenerateAccessTokenTab(page);
  await expect(page.getByLabel("generatedAccessToken")).toContainText(
    formatUsername(username),
  );
}

export async function assertNoAccessTokenGenerated(page: Page) {
  await goToGenerateAccessTokenTab(page);
  await expect(
    page.getByRole("heading", { name: "No generated access token" }),
  ).toBeVisible();
}

export async function assertHasUserInfoGenerated(page: Page, username: string) {
  await goToUserInfoTab(page);
  await expect(page.getByLabel("generatedUserInfo")).toContainText(
    formatUsername(username),
  );
}

function formatUsername(username: string) {
  return `"preferred_username": "${username}"`;
}

async function goToIdTokenTab(page: Page) {
  await page.getByTestId("generated-id-token-tab").click();
}

export async function assertHasIdTokenGenerated(page: Page, username: string) {
  await goToIdTokenTab(page);
  await expect(page.getByLabel("generatedIdToken")).toContainText(
    formatUsername(username),
  );
}

export async function assertNoIdTokenGenerated(page: Page) {
  await goToIdTokenTab(page);
  await expect(
    page.getByRole("heading", { name: "No generated id token" }),
  ).toBeVisible();
}

async function goToUserInfoTab(page: Page) {
  await page.getByTestId("generated-user-info-tab").click();
}

export async function assertNoUserInfoGenerated(page: Page) {
  await goToUserInfoTab(page);
  await expect(
    page.getByRole("heading", { name: "No generated user info" }),
  ).toBeVisible();
}

export async function selectUser(page: Page, username: string) {
  await selectItem(page, page.getByTestId("user"), username);
}
