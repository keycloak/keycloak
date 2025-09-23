import { expect, test, type Page } from "@playwright/test";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
import { clickSaveButton } from "../utils/form.ts";
import { clickTableRowItem, clickTableToolbarItem } from "../utils/table.ts";
import { login } from "../utils/login.ts";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";

// OID4VCI field selectors
const OID4VCI_FIELDS = {
  CREDENTIAL_CONFIGURATION_ID: "attributes.vc🍺credential_configuration_id",
  CREDENTIAL_IDENTIFIER: "attributes.vc🍺credential_identifier",
  ISSUER_DID: "attributes.vc🍺issuer_did",
  EXPIRY_IN_SECONDS: "attributes.vc🍺expiry_in_seconds",
  FORMAT: "attributes.vc🍺format",
} as const;

// Test values
const TEST_VALUES = {
  CREDENTIAL_CONFIG: "test-cred-config-123",
  CREDENTIAL_ID: "test-cred-identifier",
  ISSUER_DID: "did:key:test123",
  EXPIRY_SECONDS: "86400",
  FORMAT: "jwt_vc",
} as const;

// Helper function for realm-level feature check
const isRealmVerifiableCredentialsEnabled = async (
  realm: string,
): Promise<boolean> => {
  const realmData = await adminClient.getRealm(realm);
  return realmData?.verifiableCredentialsEnabled === true;
};

// Helper function to enable realm verifiable credentials
const enableRealmVerifiableCredentials = async (
  realm: string,
): Promise<void> => {
  const realmData = await adminClient.getRealm(realm);
  await adminClient.updateRealm(realm, {
    ...realmData,
    verifiableCredentialsEnabled: true,
  });
};

// Helper function to disable realm verifiable credentials
const disableRealmVerifiableCredentials = async (
  realm: string,
): Promise<void> => {
  const realmData = await adminClient.getRealm(realm);
  await adminClient.updateRealm(realm, {
    ...realmData,
    verifiableCredentialsEnabled: false,
  });
};

// Helper function to close OID4VC info modal if it's visible
const closeOid4vcModalIfVisible = async (page: Page): Promise<void> => {
  const modal = page.getByTestId("close-button");
  if (await modal.isVisible().catch(() => false)) {
    await modal.click();
    await page.waitForLoadState("domcontentloaded");
  }
};

// Helper function to wait for and close OID4VC modal that appears on protocol selection
const handleOid4vcModal = async (page: Page): Promise<void> => {
  // Wait for modal to appear (it should appear automatically when OID4VC protocol is selected)
  await expect(page.getByTestId("close-button")).toBeVisible({ timeout: 5000 });
  await closeOid4vcModalIfVisible(page);
};

test.describe("OID4VCI Client Scope Functionality", () => {
  test("should display OID4VCI fields when protocol is selected and realm feature enabled", async ({
    page,
  }) => {
    const realm = await createTestBed();
    await login(page, { to: toClientScopes({ realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    // Wait for protocol field to be visible
    await expect(page.locator("#kc-protocol")).toBeVisible();

    // Select OID4VCI protocol
    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    const oid4vcOption = page.getByRole("option", {
      name: "OpenID for Verifiable Credentials",
    });
    await expect(oid4vcOption).toBeVisible();
    await oid4vcOption.click();

    // Wait for form to update
    await page.waitForLoadState("domcontentloaded");

    // Handle OID4VC modal if it appears (only when realm feature is enabled)
    const isRealmEnabled = await isRealmVerifiableCredentialsEnabled(realm);
    if (isRealmEnabled) {
      await handleOid4vcModal(page);
    }

    // Verify protocol selection
    await expect(page.locator("#kc-protocol")).toContainText(
      "OpenID for Verifiable Credentials",
    );

    if (isRealmEnabled) {
      // Realm feature is enabled - expect OID4VCI fields
      const oid4vcFields = page.locator('[data-testid*="vc"]');
      await expect(oid4vcFields).toHaveCount(5);

      // Verify all OID4VCI fields are present
      await expect(
        page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID),
      ).toBeVisible();
      await expect(
        page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_IDENTIFIER),
      ).toBeVisible();
      await expect(page.getByTestId(OID4VCI_FIELDS.ISSUER_DID)).toBeVisible();
      await expect(
        page.getByTestId(OID4VCI_FIELDS.EXPIRY_IN_SECONDS),
      ).toBeVisible();
      await expect(page.getByTestId(OID4VCI_FIELDS.FORMAT)).toBeVisible();
    } else {
      // Realm feature is disabled - expect alert message
      await expect(page.getByText("OID4VCI Feature Disabled")).toBeVisible();
      await expect(
        page.getByText(/The OID4VCI.*feature is not enabled for this realm/),
      ).toBeVisible();
    }
  });

  test("should save and persist OID4VCI field values when realm feature is enabled", async ({
    page,
  }) => {
    const realm = await createTestBed();
    const testClientScopeName = `oid4vci-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    // Enable realm verifiable credentials feature for this test
    await enableRealmVerifiableCredentials(realm);

    await login(page, { to: toClientScopes({ realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    // Wait for protocol field to be visible
    await expect(page.locator("#kc-protocol")).toBeVisible();

    // Select OID4VCI protocol
    const { selectItem } = await import("../utils/form.ts");
    await selectItem(page, "#kc-protocol", "OpenID for Verifiable Credentials");

    // Wait for form to update
    await page.waitForLoadState("domcontentloaded");

    // Handle OID4VC modal that appears automatically when OID4VC protocol is selected
    await handleOid4vcModal(page);

    // Fill OID4VCI field values
    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID)
      .fill(TEST_VALUES.CREDENTIAL_CONFIG);
    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_IDENTIFIER)
      .fill(TEST_VALUES.CREDENTIAL_ID);
    await page
      .getByTestId(OID4VCI_FIELDS.ISSUER_DID)
      .fill(TEST_VALUES.ISSUER_DID);
    await page
      .getByTestId(OID4VCI_FIELDS.EXPIRY_IN_SECONDS)
      .fill(TEST_VALUES.EXPIRY_SECONDS);

    // Select the format option from the dropdown
    await selectItem(page, "#kc-vc-format", "JWT VC (jwt_vc)");

    // Fill in the name field
    await page.getByTestId("name").fill(testClientScopeName);

    // Save the client scope
    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    // Navigate back to client scopes list using direct URL to avoid modal blocking
    const currentUrl = page.url();
    const baseUrl = currentUrl.split("#")[0];
    await page.goto(`${baseUrl}#${toClientScopes({ realm }).pathname!}`);
    await page.waitForLoadState("domcontentloaded");

    // Search for the created client scope
    await page
      .getByPlaceholder("Search for client scope")
      .fill(testClientScopeName);

    await clickTableRowItem(page, testClientScopeName);
    await page.waitForLoadState("domcontentloaded");

    // Verify OID4VCI fields contain saved values
    await expect(
      page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID),
    ).toHaveValue(TEST_VALUES.CREDENTIAL_CONFIG);
    await expect(
      page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_IDENTIFIER),
    ).toHaveValue(TEST_VALUES.CREDENTIAL_ID);
    await expect(page.getByTestId(OID4VCI_FIELDS.ISSUER_DID)).toHaveValue(
      TEST_VALUES.ISSUER_DID,
    );
    await expect(
      page.getByTestId(OID4VCI_FIELDS.EXPIRY_IN_SECONDS),
    ).toHaveValue(TEST_VALUES.EXPIRY_SECONDS);
    await expect(page.locator("#kc-vc-format")).toContainText(
      "JWT VC (jwt_vc)",
    );
  });

  test("should show alert when OID4VCI protocol selected but realm feature disabled", async ({
    page,
  }) => {
    const realm = await createTestBed();

    // Disable realm verifiable credentials feature for this test
    await disableRealmVerifiableCredentials(realm);

    await login(page, { to: toClientScopes({ realm }) });

    // Navigate to client scopes
    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    // Click Create client scope
    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    // Wait for the form to load
    await expect(page.locator("#kc-protocol")).toBeVisible();

    // Select OID4VCI protocol
    await page.locator("#kc-protocol").click();
    await expect(
      page.getByRole("option", { name: "OpenID for Verifiable Credentials" }),
    ).toBeVisible();
    await page
      .getByRole("option", { name: "OpenID for Verifiable Credentials" })
      .click();

    // Wait for form to update
    await page.waitForLoadState("domcontentloaded");

    // Check that the alert is shown
    await expect(page.getByText("OID4VCI Feature Disabled")).toBeVisible();
    await expect(
      page.getByText(/The OID4VCI.*feature is not enabled for this realm/),
    ).toBeVisible();
  });

  test("should not display OID4VCI fields when protocol is not OID4VCI", async ({
    page,
  }) => {
    const realm = await createTestBed();
    await login(page, { to: toClientScopes({ realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    // Wait for protocol field to be visible
    await expect(page.locator("#kc-protocol")).toBeVisible();

    // Select OpenID Connect protocol (not OID4VCI)
    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    const openidConnectOption = page.getByRole("option", {
      name: "OpenID Connect",
    });
    await expect(openidConnectOption).toBeVisible();
    await openidConnectOption.click();

    // Wait for form to update
    await page.waitForLoadState("domcontentloaded");

    // Verify OID4VCI fields are not visible
    await expect(
      page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID),
    ).toBeHidden();
    await expect(
      page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_IDENTIFIER),
    ).toBeHidden();
    await expect(page.getByTestId(OID4VCI_FIELDS.ISSUER_DID)).toBeHidden();
    await expect(
      page.getByTestId(OID4VCI_FIELDS.EXPIRY_IN_SECONDS),
    ).toBeHidden();
    await expect(page.getByTestId(OID4VCI_FIELDS.FORMAT)).toBeHidden();
  });

  test("should handle OID4VCI protocol selection correctly", async ({
    page,
  }) => {
    const realm = await createTestBed();
    await login(page, { to: toClientScopes({ realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    // Wait for protocol field to be visible
    await expect(page.locator("#kc-protocol")).toBeVisible();

    // Test protocol dropdown functionality
    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    // Verify dropdown options are available
    const oid4vcOption = page.getByRole("option", {
      name: "OpenID for Verifiable Credentials",
    });
    const openidConnectOption = page.getByRole("option", {
      name: "OpenID Connect",
    });

    await expect(oid4vcOption).toBeVisible();
    await expect(openidConnectOption).toBeVisible();

    // Select OID4VCI protocol
    await oid4vcOption.click();

    // Wait for form to update
    await page.waitForLoadState("domcontentloaded");

    // Verify protocol selection
    await expect(page.locator("#kc-protocol")).toContainText(
      "OpenID for Verifiable Credentials",
    );

    // Check realm feature status to determine what should be visible
    const isRealmEnabled = await isRealmVerifiableCredentialsEnabled(realm);

    if (isRealmEnabled) {
      // Handle OID4VC modal that appears when protocol is selected
      await handleOid4vcModal(page);

      // Should see OID4VCI fields
      await expect(
        page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID),
      ).toBeVisible();
    } else {
      // Should see alert message
      await expect(page.getByText("OID4VCI Feature Disabled")).toBeVisible();
    }
  });
});
