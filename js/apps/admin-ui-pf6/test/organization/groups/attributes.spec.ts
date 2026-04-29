import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../../utils/AdminClient.ts";
import {
  assertAttributeLength,
  clickAttributeSaveButton,
  deleteAttribute,
  fillAttributeData,
  goToAttributesTab,
} from "../../utils/attributes.ts";
import { login } from "../../utils/login.ts";
import { assertNotificationMessage } from "../../utils/masthead.ts";
import { goToOrganizations, goToRealm } from "../../utils/sidebar.ts";
import { clickTableRowItem } from "../../utils/table.ts";
import { goToGroupDetails } from "../../groups/util.ts";
import { goToOrgGroupsTab } from "../groups.ts";

test.describe.serial("Org Group Attributes", () => {
  const realmName = `org-groups-attr-${uuid()}`;
  const orgName = `org-${uuid()}`;
  const groupName = `group-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { organizationsEnabled: true });
    await adminClient.createOrganization({
      realm: realmName,
      name: orgName,
      domains: [{ name: `${orgName}.org`, verified: false }],
    });
    await adminClient.createOrgGroup(orgName, groupName, realmName);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToOrganizations(page);
    await clickTableRowItem(page, orgName);
    await goToOrgGroupsTab(page);
    await goToGroupDetails(page, groupName);
    await goToAttributesTab(page);
  });

  test("Add/remove attribute", async ({ page }) => {
    await fillAttributeData(page, "key", "value");
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "Group updated");
    await assertAttributeLength(page, 1);

    await deleteAttribute(page, 0);
    await clickAttributeSaveButton(page);
    await assertNotificationMessage(page, "Group updated");
    await assertAttributeLength(page, 0);
  });
});
