import type { Page } from "@playwright/test";

export async function goToCreate(page: Page, empty: boolean = true) {
  await page
    .getByTestId(empty ? "no-workflows-empty-action" : "create-workflow")
    .click();
}

export async function fillCreatePage(page: Page, yaml: string) {
  await fillYamlField(page, yaml);
}

export function getYamlField(page: Page) {
  return page.getByTestId("workflowYAML");
}

export async function fillYamlField(page: Page, yaml: string) {
  await getYamlField(page).clear();
  await getYamlField(page).fill(yaml);
}
