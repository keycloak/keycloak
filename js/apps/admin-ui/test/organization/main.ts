import type { Page } from "@playwright/test";

export async function goToCreate(page: Page, empty: boolean = true) {
  await page
    .getByTestId(empty ? "no-organizations-empty-action" : "addOrganization")
    .click();
}

export async function fillCreatePage(
  page: Page,
  values: { name: string; domain?: string[]; description?: string },
) {
  await fillNameField(page, values.name);
  if (values.domain) {
    for (let index = 0; index < values.domain.length; index++) {
      await page.getByTestId(`domains${index}`).fill(values.domain[index]);
      if (index !== values.domain.length - 1) {
        await page.getByTestId("domains-addValue").click();
      }
    }
  }
  if (values.description) {
    await page.getByTestId("description").fill(values.description);
  }
}

export function getNameField(page: Page) {
  return page.getByTestId("name");
}

export async function fillNameField(page: Page, name: string) {
  await getNameField(page).clear();
  await getNameField(page).fill(name);
}
