import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { toUser } from "../../src/user/routes/User.tsx";
import {
  assertRowExists,
  getTableData,
  openRowDetails,
} from "../utils/table.ts";

function statusWorkflowStr(status: string): string {
  return `
    name: Set all new users to ${status} status
    on: user_created
    steps:
      - uses: set-user-attribute
        with:
          ${status}-status: true
      - uses: disable-user
        after: 60d
   `;
}

test.describe.serial("Workflow tab in Users section", () => {
  const realmName = `workflow-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createWorkflowAsYaml(
      realmName,
      statusWorkflowStr("Gold"),
    );
    await adminClient.createWorkflowAsYaml(
      realmName,
      statusWorkflowStr("Silver"),
    );
    await adminClient.createUser({
      realm: realmName,
      username: "test-user",
      enabled: true,
    });
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    const user = await adminClient.findUserByUsername(realmName, "test-user");
    await login(page, {
      to: toUser({ realm: realmName, id: user.id!, tab: "workflows" }),
    });
  });

  test("should show pending workflow details", async ({ page }) => {
    const goldStatusName = "Set all new users to Gold status";
    const silverStatusName = "Set all new users to Silver status";

    await assertRowExists(page, goldStatusName);
    await assertRowExists(page, silverStatusName);

    await openRowDetails(page, silverStatusName);
    await openRowDetails(page, goldStatusName);

    await page.getByTestId(`yaml-ex-toggle-${silverStatusName}`).click();
    await page.getByTestId(`yaml-ex-toggle-${goldStatusName}`).click();

    await expect(
      page.getByTestId(`workflowYAML-${silverStatusName}`),
    ).toHaveText(statusWorkflowStr("Silver"));

    await expect(page.getByTestId(`workflowYAML-${goldStatusName}`)).toHaveText(
      statusWorkflowStr("Gold"),
    );

    const tableData = await getTableData(page, `${silverStatusName}-Steps`);
    expect(tableData).toHaveLength(2);
    expect(tableData[0][0]).toBe("set-user-attribute");
    expect(tableData[0][1]).toBe("");
    expect(tableData[0][2]).toBe("Completed");
    expect(tableData[1][0]).toBe("disable-user");
    expect(new Date(tableData[1][1]).getTime()).not.toBeNaN(); // test if date string is valid
    expect(tableData[1][2]).toBe("Pending");
  });
});
