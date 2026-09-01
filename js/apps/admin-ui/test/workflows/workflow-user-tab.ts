import { expect, type Page } from "@playwright/test";
import { getTableData } from "../utils/table.ts";

export async function toggleWorkflowYaml(page: Page, workflowName: string) {
  await page.getByTestId(`yaml-ex-toggle-${workflowName}`).click();
}

export async function assertWorkflowYaml(
  page: Page,
  workflowName: string,
  expectedYaml: string,
) {
  await expect(page.getByTestId(`workflowYAML-${workflowName}`)).toHaveText(
    expectedYaml,
  );
}

export async function assertPendingWorkflowSteps(
  page: Page,
  workflowName: string,
) {
  const tableData = await getTableData(page, `${workflowName}-Steps`);
  expect(tableData).toHaveLength(2);
  expect(tableData[0][0]).toBe("set-user-attribute");
  expect(tableData[0][1]).toBe("");
  expect(tableData[0][2]).toBe("Completed");
  expect(tableData[1][0]).toBe("disable-user");
  expect(new Date(tableData[1][1]).getTime()).not.toBeNaN();
  expect(tableData[1][2]).toBe("Pending");
}
