import test from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { clickSaveButton } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { goToRealm } from "../utils/sidebar.ts";
import { goToPermissions } from "./main.ts";
import {
  clickCreateNewPolicy,
  clickPolicyType,
  fillPolicyForm,
  goToPolicies,
} from "./policy.ts";

test.describe.serial("Policy section tests", () => {
  const realmName = `permissions-policy-${uuid()}`;

  test.beforeAll(() =>
    adminClient.createRealm(realmName, { adminPermissionsEnabled: true }),
  );
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToPermissions(page);
    await goToPolicies(page);
  });

  test("create policy", async ({ page }) => {
    await clickCreateNewPolicy(page);
    await clickPolicyType(page, "Client");
    await fillPolicyForm(page, {
      name: "test-policy",
      description: "test-description",
      client: "broker",
    });
    await clickSaveButton(page);
  });
});
