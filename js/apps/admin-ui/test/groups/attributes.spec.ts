import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  deleteAttribute,
  fillAttributeData,
  goToAttributesTab,
} from "../utils/attributes";
import { login } from "../utils/login";
import { assertNotificationMessage } from "../utils/masthead";
import { goToGroups } from "../utils/sidebar";
import { goToGroupDetails } from "./util";

test.describe("Attributes", () => {
  const groupName = `group-${uuid()}`;

  test.beforeAll(() => adminClient.createGroup(groupName));

  test.afterAll(() => adminClient.deleteGroups());

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToGroups(page);
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
