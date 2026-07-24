import { test } from "@playwright/test";
import { toAuthentication } from "../../src/authentication/routes/Authentication.tsx";
import { createTestBed } from "../support/testbed.ts";
import {
  assertFieldError,
  assertRequiredFieldError,
  clickSaveButton,
} from "../utils/form.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
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

test.describe("Authentication - Policies - CIBA", () => {
  test("displays the initial state", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToCIBAPolicyTab(page);
    // Check initial select value
    await assertBackchannelTokenDeliveryMode(page, "Poll");

    await assertExpiresInput(page, 120);
    await assertIntervalInput(page, 5);

    await assertSaveButtonDisabled(page);
  });

  test("validates the fields", async ({ page }) => {
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToCIBAPolicyTab(page);

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
    await using testBed = await createTestBed();

    await login(page, {
      to: toAuthentication({ realm: testBed.realm, tab: "policies" }),
    });
    await goToCIBAPolicyTab(page);

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
