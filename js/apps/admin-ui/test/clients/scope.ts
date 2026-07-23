import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function goToClientScopesTab(page: Page) {
  await page.getByTestId("clientScopesTab").click();
}

export async function goToClientScopeEvaluateTab(page: Page) {
  await page.getByTestId("clientScopesEvaluateTab").click();
}

export async function clickAddClientScope(page: Page) {
  const toolbar = page.getByTestId("table-toolbar");
  await expect(toolbar).toBeVisible();
  const directAction = [
    toolbar.getByRole("button", { name: /Add client scope/i }),
    toolbar.getByRole("link", { name: /Add client scope/i }),
    toolbar.getByRole("button", { name: /Add scope/i }),
    toolbar.getByRole("link", { name: /Add scope/i }),
    toolbar.getByRole("button", { name: /^Add$/i }),
    toolbar.getByRole("link", { name: /^Add$/i }),
  ];

  for (const action of directAction) {
    const actionCount = await action.count();
    for (let i = 0; i < actionCount; i++) {
      const candidate = action.nth(i);
      if (await candidate.isVisible()) {
        await candidate.click();
        return;
      }
    }
  }

  if ((await toolbar.getByTestId("kebab").count()) > 0) {
    await toolbar.getByTestId("kebab").first().click();
    const menuAction = [
      page.getByRole("menuitem", { name: /Add client scope/i }),
      page.getByRole("menuitem", { name: /Add scope/i }),
      page.getByRole("menuitem", { name: /^Add$/i }),
    ];
    for (const action of menuAction) {
      if ((await action.count()) > 0) {
        await action.first().click();
        return;
      }
    }
  }
  throw new Error(
    `Could not find add scope action in toolbar: ${await toolbar.textContent()}`,
  );
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
