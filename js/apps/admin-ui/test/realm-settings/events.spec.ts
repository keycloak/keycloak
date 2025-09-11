import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { switchOn } from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { assertModalMessage, confirmModal } from "../utils/modal.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import { assertRowExists, searchItem } from "../utils/table.ts";
import {
  addSavedEventTypes,
  clickClearEvents,
  clickRemoveListener,
  clickSaveEventsConfig,
  clickSaveEventsListener,
  fillEventListener,
  goToEventsTab,
  goToRealmEventsTab,
} from "./events.ts";

test.describe.serial("Realm settings events tab tests", () => {
  const realmName = `events-realm-settings-${crypto.randomUUID()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
    await goToRealmEventsTab(page);
  });

  test("Enable user events", async ({ page }) => {
    await goToEventsTab(page);

    await switchOn(page, "[data-testid='eventsEnabled']");
    await clickSaveEventsConfig(page);
    await assertNotificationMessage(page, "Successfully saved configuration");

    await clickClearEvents(page);
    await assertModalMessage(
      page,
      "If you clear all events of this realm, all records will be permanently cleared in the database",
    );
    await confirmModal(page);
    await assertNotificationMessage(page, "The user events have been cleared");

    const eventTypes = ["Identity provider response", "Client info error"];
    await addSavedEventTypes(page, eventTypes);

    await assertNotificationMessage(page, "Successfully saved configuration");

    for (const event of eventTypes) {
      await searchItem(page, "Search saved event type", event);
      await assertRowExists(page, event);
    }
  });

  test("Should revert saving event listener", async ({ page }) => {
    await fillEventListener(page, "email");
    await page.getByTestId("revertEventListenerBtn").click();
  });

  test("Should save event listener", async ({ page }) => {
    await fillEventListener(page, "email");
    await clickSaveEventsListener(page);
    await assertNotificationMessage(page, "Event listener has been updated.");
  });

  test("Should remove event from event listener", async ({ page }) => {
    await clickRemoveListener(page, "jboss-logging");
    await clickSaveEventsListener(page);
    await assertNotificationMessage(page, "Event listener has been updated.");
  });
});
