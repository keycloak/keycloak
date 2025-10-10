import { expect, test } from "@playwright/test";
import { generatePath } from "react-router-dom";
import { toRealmSettings } from "../../src/realm-settings/routes/RealmSettings.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.js";
import { SERVER_URL, ROOT_PATH } from "../utils/constants.ts";
import { login } from "../utils/login.js";

test("OID4VCI section visibility and jump link in Tokens tab", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

  const tokensTab = page.getByTestId("rs-tokens-tab");
  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await expect(oid4vciJumpLink).toBeVisible();

  await oid4vciJumpLink.click();
  const oid4vciSection = page.getByRole("heading", {
    name: "OID4VCI attributes",
  });
  await expect(oid4vciSection).toBeVisible();
});

test("should render fields and save values with correct attribute keys", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

  const tokensTab = page.getByTestId("rs-tokens-tab");
  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await oid4vciJumpLink.click();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );

  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();

  await nonceField.fill("60");
  await preAuthField.fill("120");
  await page.getByTestId("tokens-tab-save").click();
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();

  const realmData = await adminClient.getRealm(testBed.realm);
  expect(realmData).toBeDefined();
  // TimeSelector converts values based on selected unit (60 minutes = 3600 seconds, 120 seconds = 120 seconds)
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBe("3600");
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBe("120");
});

test("should persist values after page refresh", async ({ page }) => {
  await using testBed = await createTestBed();
  await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

  const tokensTab = page.getByTestId("rs-tokens-tab");
  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await oid4vciJumpLink.click();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );

  await nonceField.fill("60");
  await preAuthField.fill("120");
  await page.getByTestId("tokens-tab-save").click();
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();

  // Refresh the page
  await page.reload();

  // Navigate back to realm settings using the same pattern as login
  const url = new URL(
    generatePath(ROOT_PATH, { realm: testBed.realm }),
    SERVER_URL,
  );
  url.hash = toRealmSettings({ realm: testBed.realm }).pathname!;
  await page.goto(url.toString());

  // The TimeSelector component converts values based on units, so we need to check the actual saved values
  const realmData = await adminClient.getRealm(testBed.realm);
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBeDefined();
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBeDefined();

  // The values should be numbers representing seconds
  const nonceValue = parseInt(
    realmData?.attributes?.["vc.c-nonce-lifetime-seconds"] || "0",
  );
  const preAuthValue = parseInt(
    realmData?.attributes?.["preAuthorizedCodeLifespanS"] || "0",
  );

  expect(nonceValue).toBeGreaterThan(0);
  expect(preAuthValue).toBeGreaterThan(0);
});

test("should validate form fields and save valid values", async ({ page }) => {
  await using testBed = await createTestBed();
  await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

  const tokensTab = page.getByTestId("rs-tokens-tab");
  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await oid4vciJumpLink.click();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );
  const saveButton = page.getByTestId("tokens-tab-save");

  // Test that fields are visible and can be filled
  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();
  await expect(saveButton).toBeVisible();

  // Test with valid values - this should work
  await nonceField.clear();
  await preAuthField.clear();

  // Fill with smaller, more reasonable values for testing
  await nonceField.fill("60");
  await preAuthField.fill("120");

  // Save button should be enabled when form has values
  await expect(saveButton).toBeEnabled();

  await saveButton.click();
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();

  // Verify the values were saved correctly
  const realmData = await adminClient.getRealm(testBed.realm);
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBeDefined();
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBeDefined();

  // The values should be numbers representing seconds
  const nonceValue = parseInt(
    realmData?.attributes?.["vc.c-nonce-lifetime-seconds"] || "0",
  );
  const preAuthValue = parseInt(
    realmData?.attributes?.["preAuthorizedCodeLifespanS"] || "0",
  );

  expect(nonceValue).toBeGreaterThan(0);
  expect(preAuthValue).toBeGreaterThan(0);
});

test("should show validation error for values below minimum threshold", async ({
  page,
}) => {
  await using testBed = await createTestBed();
  await login(page, { to: toRealmSettings({ realm: testBed.realm }) });

  const tokensTab = page.getByTestId("rs-tokens-tab");
  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await oid4vciJumpLink.click();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );
  const saveButton = page.getByTestId("tokens-tab-save");

  // Fill with values below the minimum threshold (29 seconds)
  await nonceField.fill("29");
  await preAuthField.fill("29");

  await saveButton.click();

  // Check for validation error message
  const validationErrorText =
    "Please ensure the OID4VCI attribute fields are filled with values 30 seconds or greater.";
  await expect(page.getByText(validationErrorText).first()).toBeVisible();
});
