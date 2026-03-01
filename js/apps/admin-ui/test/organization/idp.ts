import type { Page } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function goToIdentityProviders(page: Page) {
  await page.getByTestId("identityProvidersTab").click();
}

export async function clickAddIdentityProvider(page: Page) {
  await page
    .getByTestId("no-identity-provider-in-this-organization-empty-action")
    .click();
}

export async function fillForm(
  page: Page,
  data: {
    name: string;
    domain: string;
  },
) {
  await selectItem(page, page.getByTestId("alias"), data.name);
  await selectItem(
    page,
    page.locator("#config\\.kc🍺org🍺domain"),
    data.domain,
  );
}
