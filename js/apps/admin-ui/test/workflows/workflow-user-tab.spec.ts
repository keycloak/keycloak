import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { toUser } from "../../src/user/routes/User.tsx";
import { assertRowExists, openRowDetails } from "../utils/table.ts";
import {
  assertPendingWorkflowSteps,
  assertWorkflowYaml,
  toggleWorkflowYaml,
} from "./workflow-user-tab.ts";

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

    await toggleWorkflowYaml(page, silverStatusName);
    await toggleWorkflowYaml(page, goldStatusName);

    await assertWorkflowYaml(
      page,
      silverStatusName,
      statusWorkflowStr("Silver"),
    );
    await assertWorkflowYaml(page, goldStatusName, statusWorkflowStr("Gold"));
    await assertPendingWorkflowSteps(page, silverStatusName);
  });
});
