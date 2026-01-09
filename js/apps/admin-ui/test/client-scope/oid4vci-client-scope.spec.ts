import { expect, test } from "@playwright/test";
import type { Page } from "@playwright/test";
import { createTestBed } from "../support/testbed.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
import { clickSaveButton, selectItem } from "../utils/form.ts";
import { clickTableRowItem, clickTableToolbarItem } from "../utils/table.ts";
import { login } from "../utils/login.ts";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";

// Helper function to create client scope (without selecting protocol)
async function createClientScope(
  page: Page,
  testBed: Awaited<ReturnType<typeof createTestBed>>,
) {
  await login(page, { to: toClientScopes({ realm: testBed.realm }) });

  await goToClientScopes(page);
  await page.waitForLoadState("domcontentloaded");

  await clickTableToolbarItem(page, "Create client scope");
  await page.waitForLoadState("domcontentloaded");
}

// Helper function to create client scope and select protocol/format
async function createClientScopeAndSelectProtocolAndFormat(
  page: Page,
  testBed: Awaited<ReturnType<typeof createTestBed>>,
  format?: "SD-JWT VC (dc+sd-jwt)" | "JWT VC (jwt_vc)",
) {
  await createClientScope(page, testBed);

  await selectItem(page, "#kc-protocol", "OpenID for Verifiable Credentials");

  await page.waitForLoadState("domcontentloaded");

  if (format) {
    await selectItem(page, "#kc-vc-format", format);
    await page.waitForLoadState("domcontentloaded");
  }
}

// Helper function to navigate back to client scope and verify saved values
async function navigateBackAndVerifyClientScope(
  page: Page,
  testBed: Awaited<ReturnType<typeof createTestBed>>,
  clientScopeName: string,
) {
  const currentUrl = page.url();
  const baseUrl = currentUrl.split("#")[0];
  await page.goto(
    `${baseUrl}#${toClientScopes({ realm: testBed.realm }).pathname!}`,
  );
  await page.waitForLoadState("domcontentloaded");

  await page.getByPlaceholder("Search for client scope").fill(clientScopeName);

  await clickTableRowItem(page, clientScopeName);
  await page.waitForLoadState("domcontentloaded");
}

// OID4VCI field selectors
const OID4VCI_FIELDS = {
  CREDENTIAL_CONFIGURATION_ID: "attributes.vc🍺credential_configuration_id",
  CREDENTIAL_IDENTIFIER: "attributes.vc🍺credential_identifier",
  ISSUER_DID: "attributes.vc🍺issuer_did",
  EXPIRY_IN_SECONDS: "attributes.vc🍺expiry_in_seconds",
  FORMAT: "#kc-vc-format",
  TOKEN_JWS_TYPE: "attributes.vc🍺credential_build_config🍺token_jws_type",
  SIGNING_KEY_ID: "#kc-signing-key-id",
  SIGNING_ALGORITHM: "#kc-credential-signing-alg",
  HASH_ALGORITHM: "#kc-hash-algorithm",
  DISPLAY: "attributes.vc🍺display",
  SUPPORTED_CREDENTIAL_TYPES: "attributes.vc🍺supported_credential_types",
  VERIFIABLE_CREDENTIAL_TYPE: "attributes.vc🍺verifiable_credential_type",
  VISIBLE_CLAIMS:
    "attributes.vc🍺credential_build_config🍺sd_jwt🍺visible_claims",
} as const;

// Test values
const TEST_VALUES = {
  CREDENTIAL_CONFIG: "test-cred-config-123",
  CREDENTIAL_ID: "test-cred-identifier",
  ISSUER_DID: "did:key:test123",
  EXPIRY_SECONDS: "86400",
  SIGNING_ALG: "ES256",
  HASH_ALGORITHM: "SHA-384",
  TOKEN_JWS_TYPE: "dc+sd-jwt",
  VISIBLE_CLAIMS: "id,iat,nbf,exp,jti,given_name",
  DISPLAY:
    '[{"name": "Test Credential", "locale": "en-US", "logo": {"uri": "https://example.com/logo.png", "alt_text": "Logo"}, "background_color": "#12107c", "text_color": "#FFFFFF"}]',
  SUPPORTED_CREDENTIAL_TYPES: "VerifiableCredential,UniversityDegreeCredential",
  VERIFIABLE_CREDENTIAL_TYPE: "TestCredentialType",
} as const;

test.describe("OID4VCI Client Scope Functionality", () => {
  test("should display OID4VCI fields when protocol is selected", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScope(page, testBed);

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
    await expect(page.getByTestId(OID4VCI_FIELDS.TOKEN_JWS_TYPE)).toBeVisible();
    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toBeVisible();
    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toBeVisible();
    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toBeVisible();
  });

  test("should save and persist OID4VCI field values", async ({ page }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "JWT VC (jwt_vc)",
    );

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

    await page
      .getByTestId(OID4VCI_FIELDS.TOKEN_JWS_TYPE)
      .fill(TEST_VALUES.TOKEN_JWS_TYPE);
    await selectItem(
      page,
      OID4VCI_FIELDS.SIGNING_ALGORITHM,
      TEST_VALUES.SIGNING_ALG,
    );

    await selectItem(
      page,
      OID4VCI_FIELDS.HASH_ALGORITHM,
      TEST_VALUES.HASH_ALGORITHM,
    );

    await page.getByTestId(OID4VCI_FIELDS.DISPLAY).fill(TEST_VALUES.DISPLAY);
    await page
      .getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES)
      .fill(TEST_VALUES.SUPPORTED_CREDENTIAL_TYPES);

    await page.getByTestId("name").fill(testClientScopeName);

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    await navigateBackAndVerifyClientScope(page, testBed, testClientScopeName);

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
    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toContainText(
      TEST_VALUES.SIGNING_ALG,
    );
    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toContainText(
      TEST_VALUES.HASH_ALGORITHM,
    );
    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toHaveValue(
      TEST_VALUES.DISPLAY,
    );
    await expect(
      page.getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES),
    ).toHaveValue(TEST_VALUES.SUPPORTED_CREDENTIAL_TYPES);
    await expect(page.getByTestId(OID4VCI_FIELDS.TOKEN_JWS_TYPE)).toHaveValue(
      TEST_VALUES.TOKEN_JWS_TYPE,
    );
  });

  test("should show OID4VCI protocol when global feature is enabled", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScope(page, testBed);

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
    await createClientScope(page, testBed);

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
    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toBeHidden();
    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toBeHidden();
    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toBeHidden();
  });

  test("should handle OID4VCI protocol selection correctly", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScope(page, testBed);

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

  test("should only show supported format options (dc+sd-jwt and jwt_vc)", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(page, testBed);

    await page.locator("#kc-vc-format").click();

    await expect(
      page.getByRole("option", { name: "SD-JWT VC (dc+sd-jwt)" }),
    ).toBeVisible();
    await expect(
      page.getByRole("option", { name: "JWT VC (jwt_vc)" }),
    ).toBeVisible();

    await expect(
      page.getByRole("option", { name: "LDP VC (ldp_vc)" }),
    ).toBeHidden();
  });

  test("should show format-specific fields for SD-JWT format", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toBeVisible();
    await expect(
      page.getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES),
    ).toBeVisible();

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeVisible();
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toBeVisible();
  });

  test("should show format-specific fields for JWT VC format", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "JWT VC (jwt_vc)",
    );

    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toBeVisible();
    await expect(
      page.getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES),
    ).toBeVisible();

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeHidden();
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toBeHidden();
  });

  test("should save and persist new OID4VCI field values for SD-JWT format", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-sdjwt-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID)
      .fill(TEST_VALUES.CREDENTIAL_CONFIG);
    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_IDENTIFIER)
      .fill(TEST_VALUES.CREDENTIAL_ID);

    await selectItem(
      page,
      OID4VCI_FIELDS.SIGNING_ALGORITHM,
      TEST_VALUES.SIGNING_ALG,
    );

    await page.getByTestId(OID4VCI_FIELDS.DISPLAY).fill(TEST_VALUES.DISPLAY);
    await page
      .getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES)
      .fill(TEST_VALUES.SUPPORTED_CREDENTIAL_TYPES);
    await page
      .getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE)
      .fill(TEST_VALUES.VERIFIABLE_CREDENTIAL_TYPE);
    await page
      .getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)
      .fill(TEST_VALUES.VISIBLE_CLAIMS);

    await page.getByTestId("name").fill(testClientScopeName);

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    await navigateBackAndVerifyClientScope(page, testBed, testClientScopeName);

    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toHaveValue(
      TEST_VALUES.DISPLAY,
    );
    await expect(
      page.getByTestId(OID4VCI_FIELDS.SUPPORTED_CREDENTIAL_TYPES),
    ).toHaveValue(TEST_VALUES.SUPPORTED_CREDENTIAL_TYPES);
    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toHaveValue(TEST_VALUES.VERIFIABLE_CREDENTIAL_TYPE);
    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toContainText(
      TEST_VALUES.SIGNING_ALG,
    );
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toHaveValue(
      TEST_VALUES.VISIBLE_CLAIMS,
    );
    await expect(page.locator("#kc-vc-format")).toContainText(
      "SD-JWT VC (dc+sd-jwt)",
    );
  });

  test("should omit optional OID4VCI fields when left blank", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-blank-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await page.getByTestId("name").fill(testClientScopeName);

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    await navigateBackAndVerifyClientScope(page, testBed, testClientScopeName);

    await expect(page.getByTestId(OID4VCI_FIELDS.ISSUER_DID)).toHaveValue("");
    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toHaveText("");
    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toContainText(
      "SHA-256",
    );
    await expect(page.getByTestId(OID4VCI_FIELDS.DISPLAY)).toHaveValue("");
  });

  test("should conditionally show/hide fields when format changes", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeVisible();

    await selectItem(page, "#kc-vc-format", "JWT VC (jwt_vc)");

    await page.waitForLoadState("domcontentloaded");

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeHidden();

    await selectItem(page, "#kc-vc-format", "SD-JWT VC (dc+sd-jwt)");

    await page.waitForLoadState("domcontentloaded");

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeVisible();
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toBeVisible();

    await selectItem(page, "#kc-vc-format", "JWT VC (jwt_vc)");

    await page.waitForLoadState("domcontentloaded");

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeHidden();
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toBeHidden();

    await selectItem(page, "#kc-vc-format", "SD-JWT VC (dc+sd-jwt)");

    await page.waitForLoadState("domcontentloaded");

    await expect(
      page.getByTestId(OID4VCI_FIELDS.VERIFIABLE_CREDENTIAL_TYPE),
    ).toBeVisible();
    await expect(page.getByTestId(OID4VCI_FIELDS.VISIBLE_CLAIMS)).toBeVisible();
  });

  test("should show token_jws_type for all formats", async ({ page }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "JWT VC (jwt_vc)",
    );

    await expect(page.getByTestId(OID4VCI_FIELDS.TOKEN_JWS_TYPE)).toBeVisible();

    await selectItem(page, "#kc-vc-format", "SD-JWT VC (dc+sd-jwt)");
    await page.waitForLoadState("domcontentloaded");

    await expect(page.getByTestId(OID4VCI_FIELDS.TOKEN_JWS_TYPE)).toBeVisible();
  });

  test("should display signing algorithm dropdown with available algorithms", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await expect(page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM)).toBeVisible();

    await page.locator(OID4VCI_FIELDS.SIGNING_ALGORITHM).click();

    await expect(page.getByRole("option", { name: "RS256" })).toBeVisible();
    await expect(page.getByRole("option", { name: "ES256" })).toBeVisible();
  });

  test("should display hash algorithm dropdown with available algorithms", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toBeVisible();

    await page.locator(OID4VCI_FIELDS.HASH_ALGORITHM).click();

    await expect(page.getByRole("option", { name: "SHA-256" })).toBeVisible();
    await expect(page.getByRole("option", { name: "SHA-384" })).toBeVisible();
    await expect(page.getByRole("option", { name: "SHA-512" })).toBeVisible();
  });

  test("should save and persist hash algorithm value", async ({ page }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-hash-alg-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID)
      .fill(TEST_VALUES.CREDENTIAL_CONFIG);
    await page.getByTestId("name").fill(testClientScopeName);

    await selectItem(
      page,
      OID4VCI_FIELDS.HASH_ALGORITHM,
      TEST_VALUES.HASH_ALGORITHM,
    );

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    await navigateBackAndVerifyClientScope(page, testBed, testClientScopeName);

    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toContainText(
      TEST_VALUES.HASH_ALGORITHM,
    );
  });

  test("should default to SHA-256 when hash algorithm is not set", async ({
    page,
  }) => {
    await using testBed = await createTestBed();
    const testClientScopeName = `oid4vci-hash-default-test-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

    await createClientScopeAndSelectProtocolAndFormat(
      page,
      testBed,
      "SD-JWT VC (dc+sd-jwt)",
    );

    await page
      .getByTestId(OID4VCI_FIELDS.CREDENTIAL_CONFIGURATION_ID)
      .fill(TEST_VALUES.CREDENTIAL_CONFIG);
    await page.getByTestId("name").fill(testClientScopeName);

    await clickSaveButton(page);
    await expect(page.getByText("Client scope created")).toBeVisible();

    await navigateBackAndVerifyClientScope(page, testBed, testClientScopeName);

    await expect(page.locator(OID4VCI_FIELDS.HASH_ALGORITHM)).toContainText(
      "SHA-256",
    );
  });
});
