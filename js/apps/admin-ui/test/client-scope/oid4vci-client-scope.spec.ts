import { expect, test } from "@playwright/test";
import { createTestBed } from "../support/testbed.ts";
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
  FORMAT: "#kc-vc-format",
} as const;

// Test values
const TEST_VALUES = {
  CREDENTIAL_CONFIG: "test-cred-config-123",
  CREDENTIAL_ID: "test-cred-identifier",
  ISSUER_DID: "did:key:test123",
  EXPIRY_SECONDS: "86400",
  FORMAT: "jwt_vc",
} as const;

test.describe("OID4VCI Client Scope Functionality", () => {
  test("should display OID4VCI fields when protocol is selected", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toBeVisible();

    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    const oid4vcOption = page.getByRole("option", {
      name: "OpenID for Verifiable Credentials",
    });
    await expect(oid4vcOption).toBeVisible();
    await oid4vcOption.click();

    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toContainText(
      "OpenID for Verifiable Credentials",
    );

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
    await expect(page.locator(OID4VCI_FIELDS.FORMAT)).toBeVisible();
  });

  test("should save and persist OID4VCI field values", async ({ page }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toBeVisible();

    const { selectItem } = await import("../utils/form.ts");
    await selectItem(page, "#kc-protocol", "OpenID for Verifiable Credentials");

    await page.waitForLoadState("domcontentloaded");

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

    await selectItem(page, "#kc-vc-format", "JWT VC (jwt_vc)");

    await page.getByTestId("name").fill(testClientScopeName);

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    const currentUrl = page.url();
    const baseUrl = currentUrl.split("#")[0];
    await page.goto(
      `${baseUrl}#${toClientScopes({ realm: testBed.realm }).pathname!}`,
    );
    await page.waitForLoadState("domcontentloaded");

    await page
      .getByPlaceholder("Search for client scope")
      .fill(testClientScopeName);

    await clickTableRowItem(page, testClientScopeName);
    await page.waitForLoadState("domcontentloaded");

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

  test("should show OID4VCI protocol when global feature is enabled", async ({
    page,
  }) => {
    await using testBed = await createTestBed();

    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toBeVisible();

    await page.locator("#kc-protocol").click();

    await expect(
      page.getByRole("option", { name: "OpenID for Verifiable Credentials" }),
    ).toBeVisible();
  });

  test("should not display OID4VCI fields when protocol is not OID4VCI", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toBeVisible();

    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    const openidConnectOption = page.getByRole("option", {
      name: "OpenID Connect",
    });
    await expect(openidConnectOption).toBeVisible();
    await openidConnectOption.click();

    await page.waitForLoadState("domcontentloaded");

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
    await expect(page.locator(OID4VCI_FIELDS.FORMAT)).toBeHidden();
  });

  test("should handle OID4VCI protocol selection correctly", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await login(page, { to: toClientScopes({ realm: testBed.realm }) });

    await goToClientScopes(page);
    await page.waitForLoadState("domcontentloaded");

    await clickTableToolbarItem(page, "Create client scope");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toBeVisible();

    const protocolButton = page.locator("#kc-protocol");
    await protocolButton.click();

    const oid4vcOption = page.getByRole("option", {
      name: "OpenID for Verifiable Credentials",
    });
    const openidConnectOption = page.getByRole("option", {
      name: "OpenID Connect",
    });

    await expect(oid4vcOption).toBeVisible();
    await expect(openidConnectOption).toBeVisible();

    await oid4vcOption.click();

    await page.waitForLoadState("domcontentloaded");

    await expect(page.locator("#kc-protocol")).toContainText(
      "OpenID for Verifiable Credentials",
    );

    await expect(
      page.getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID),
    ).toBeVisible();
  });
});
