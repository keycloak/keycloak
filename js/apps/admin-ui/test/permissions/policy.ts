import type { Page } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

export async function clickCreateNewPolicy(page: Page) {
  await page.getByTestId("no-policies-empty-action").click();
}

export async function goToPolicies(page: Page) {
  await page.getByTestId("permissionsPolicies").click();
}

export async function clickPolicyType(page: Page, type: string) {
  await page.getByRole("gridcell", { name: type, exact: true }).click();
}

type PolicyForm = {
  name: string;
  description: string;
  type?: string;
  user?: string;
  client?: string;
};

export async function fillPolicyForm(
  page: Page,
  data: PolicyForm,
  dialog: boolean = false,
) {
  const entries = Object.entries(data);
  for (const [key, value] of entries) {
    if (key === "type") {
      await selectItem(page, "#type", value);
      continue;
    }
    if (key === "user") {
      await selectItem(
        page,
        page.getByRole("combobox", { name: "Type to filter" }),
        value,
      );
      continue;
    }
    if (key === "client") {
      await selectItem(page, "#clients", value);
      await page.locator("#clients").click();
      continue;
    }

    const locator = dialog
      ? page.getByRole("dialog", { name: "Create policy" })
      : page;
    await locator.getByTestId(key).fill(value);
  }
}
