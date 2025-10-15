import { test } from "@playwright/test";
import { v4 as uuidv4 } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import {
  assertFieldError,
  assertRequiredFieldError,
  clickSaveButton,
} from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToAuthentication, goToRealm } from "../utils/sidebar.ts";
import {
  assertBackchannelTokenDeliveryMode,
  assertExpiresInput,
  assertIntervalInput,
  assertSaveButtonDisabled,
  clearExpiresInput,
  clearIntervalInput,
  expiresInput,
  goToCIBAPolicyTab,
  intervalInput,
  setBackchannelTokenDeliveryMode,
  setExpiresInput,
  setIntervalInput,
} from "./policies-ciba.ts";

test.describe.serial("Authentication - Policies - CIBA", () => {
  const realmName = `authentication-policies-${uuidv4()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName);
  });

  test.afterAll(async () => {
    await adminClient.deleteRealm(realmName);
  });

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToAuthentication(page);
    await goToCIBAPolicyTab(page);
  });

  test("displays the initial state", async ({ page }) => {
    // Check initial select value
    await assertBackchannelTokenDeliveryMode(page, "Poll");

    await assertExpiresInput(page, 120);
    await assertIntervalInput(page, 5);

    await assertSaveButtonDisabled(page);
  });

  test("validates the fields", async ({ page }) => {
    // Test required fields
    await clearExpiresInput(page);
    await clearIntervalInput(page);

    await assertRequiredFieldError(page, expiresInput);
    await assertRequiredFieldError(page, intervalInput);

    await assertSaveButtonDisabled(page);

    // Test minimum values
    await setExpiresInput(page, 9);
    await setIntervalInput(page, -1);

    await assertFieldError(page, expiresInput, "Must be greater than 10");
    await assertFieldError(page, intervalInput, "Must be greater than 0");

    await assertSaveButtonDisabled(page);

    // // Test maximum values

    await setExpiresInput(page, 601);
    await setIntervalInput(page, 601);

    await assertFieldError(page, expiresInput, "Must be less than 600");
    await assertFieldError(page, intervalInput, "Must be less than 600");

    await assertSaveButtonDisabled(page);
  });

  test("saves the form", async ({ page }) => {
    // Set new values
    await setBackchannelTokenDeliveryMode(page, "Ping");

    await setExpiresInput(page, 140);
    await setIntervalInput(page, 20);

    // Save and verify success
    await clickSaveButton(page);
    await assertNotificationMessage(page, "CIBA policy successfully updated");

    // Verify saved values
    await assertBackchannelTokenDeliveryMode(page, "Ping");

    await assertExpiresInput(page, 140);
    await assertIntervalInput(page, 20);
  });
});
