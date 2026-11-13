import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  deleteAttribute,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToGroupDetails } from "./util.ts";

test.describe.serial("Attributes", () => {
  const realmName = `groups-attributes-${uuid()}`;
  const groupName = `group-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.createGroup(groupName, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page, { to: { pathname: `/${realmName}/groups` } });
    await goToGroupDetails(page, groupName);
    await goToAttributesTab(page);
  });

  test("Add/remove attribute", async ({ page }) => {
    await fillAttributeData(page, "key", "value");
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "Group updated");
    await assertAttributeLength(page, 1);

    // remove attribute
    await deleteAttribute(page, 0);
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "Group updated");
    await assertAttributeLength(page, 0);
  });
});
