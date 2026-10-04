import { expect, test, type Page } from "@playwright/test";
import { toUser } from "../../src/user/routes/User.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { login } from "../utils/login.ts";
import { clickSelectRow } from "../utils/table.ts";

const TEST_CLIENT_ID = "test-app";
const TEST_REDIRECT_URI = "https://app.example.com/callback";
const REQUIRED_ACTION = "Update Password";

function getSendButton(page: Page) {
  return page
    .getByRole("dialog", { name: "Credentials Reset" })
    .getByTestId("confirm");
}

async function openCredentialResetDialog(page: Page) {
  await page.getByTestId("credentialResetBtn").click();
  const modal = page.getByTestId("credential-reset-modal");
  await expect(modal).toBeVisible();
  return modal;
}

async function selectRequiredAction(
  page: Page,
  modal: Awaited<ReturnType<typeof openCredentialResetDialog>>,
  name: string = REQUIRED_ACTION,
) {
  await modal.getByPlaceholder("Select action").click();
  const option = modal.page().getByRole("option", { name, exact: true });
  await expect(option).toBeVisible();
  await option.click();
  await expect(
    modal.getByLabel("Current selections").getByText(name, { exact: true }),
  ).toBeVisible();
  // The multi-select menu stays open and overlays the fields below,
  // dismiss it with an outside click before interacting further.
  await page
    .getByRole("dialog", { name: "Credentials Reset" })
    .getByText("Credentials Reset", { exact: true })
    .click();
}

async function selectTestClient(
  page: Page,
  modal: Awaited<ReturnType<typeof openCredentialResetDialog>>,
) {
  await modal.getByTestId("select-client-button").click();
  const clientModal = page.getByTestId("select-client-modal");
  await clientModal
    .locator("table tbody")
    .waitFor({ state: "visible", timeout: 5_000 });
  const search = clientModal.getByPlaceholder("Search for client");
  await search.fill(TEST_CLIENT_ID);
  await search.press("Enter");
  await expect(
    clientModal.getByRole("gridcell", { name: TEST_CLIENT_ID, exact: true }),
  ).toBeVisible();
  await clickSelectRow(page, "Clients", TEST_CLIENT_ID);
  const clientDialog = page.getByRole("dialog", { name: "Select client" });
  // NOTE: data-testid="select-client-modal" is on the modal body only,
  // the footer actions live outside of it, so scope by dialog role.
  const confirm = clientDialog.getByTestId("confirm");
  await expect(confirm).toBeEnabled({ timeout: 10_000 });
  await confirm.click();
  await expect(clientDialog).toHaveCount(0);
}

async function findResetUser(realm: string) {
  return await adminClient.findUserByUsername(realm, "reset-user");
}

const resetUser = {
  username: "reset-user",
  email: "reset-user@example.com",
  enabled: true,
  credentials: [{ type: "password", value: "test", temporary: false }],
};

const testClient = {
  clientId: TEST_CLIENT_ID,
  enabled: true,
  redirectUris: ["https://app.example.com/*"],
};

test.describe("Credential reset dialog", () => {
  test("shows translated client and redirect URI fields", async ({ page }) => {
    await using testBed = await createTestBed({ users: [resetUser] });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    const modal = await openCredentialResetDialog(page);

    // Regression test: labels must resolve via the message bundle,
    // raw keys must never leak into the UI.
    await expect(modal.getByText("Client", { exact: true })).toBeVisible();
    await expect(
      modal.getByText("Redirect URI", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText(/credentialReset(Client|RedirectUri)/),
    ).toHaveCount(0);
  });

  test("disables send when redirect URI has no client", async ({ page }) => {
    await using testBed = await createTestBed({
      clients: [testClient],
      users: [resetUser],
    });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    const modal = await openCredentialResetDialog(page);
    const sendButton = getSendButton(page);
    await expect(sendButton).toBeDisabled();

    await selectRequiredAction(page, modal);
    await expect(sendButton).toBeEnabled();

    await modal.getByTestId("redirectUri").fill(TEST_REDIRECT_URI);

    // Redirect without a client must block sending, the backend
    // rejects it with "Client id missing".
    await expect(sendButton).toBeDisabled();

    await selectTestClient(page, modal);
    await expect(sendButton).toBeEnabled();
  });

  test("rejects malformed redirect URIs", async ({ page }) => {
    await using testBed = await createTestBed({
      clients: [testClient],
      users: [resetUser],
    });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    const modal = await openCredentialResetDialog(page);
    await selectRequiredAction(page, modal);

    // Bare words without whitespace count as relative references (the
    // server resolves them against the client root), so malformed means
    // illegal syntax here: raw whitespace or bad percent-escapes.
    await modal.getByTestId("redirectUri").fill("not a url");

    // Malformed URI blocks sending right away via the watched form state.
    await expect(getSendButton(page)).toBeDisabled();

    // Root-relative values get the same strict syntax check the server
    // applies: raw whitespace and bad percent-escapes stay blocked.
    await modal.getByTestId("redirectUri").fill("/bad path");
    await expect(getSendButton(page)).toBeDisabled();
    await modal.getByTestId("redirectUri").fill("/bad%ZZ");
    await expect(getSendButton(page)).toBeDisabled();

    // Picking a client re-runs field validation, surfacing the message.
    await selectTestClient(page, modal);
    await expect(modal.getByTestId("redirectUri-helper")).toHaveText(
      /valid.*URI|relative path/i,
    );
    await expect(getSendButton(page)).toBeDisabled();

    // Fixing the URI must clear the stale error and re-enable sending.
    await modal.getByTestId("redirectUri").fill(TEST_REDIRECT_URI);
    await expect(modal.getByTestId("redirectUri-helper")).toHaveCount(0);
    await expect(getSendButton(page)).toBeEnabled();
  });

  test("sends client_id and redirect_uri to the API", async ({ page }) => {
    await using testBed = await createTestBed({
      clients: [testClient],
      users: [resetUser],
    });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    let requestUrl = "";
    let requestMethod = "";
    let requestBody: unknown;
    await page.route("**/execute-actions-email*", async (route) => {
      const request = route.request();
      requestUrl = request.url();
      requestMethod = request.method();
      requestBody = request.postDataJSON();
      await route.fulfill({ status: 204 });
    });

    const modal = await openCredentialResetDialog(page);
    await selectRequiredAction(page, modal);
    await selectTestClient(page, modal);

    // The dialog surfaces the selected client's registered redirect URIs
    // as a hint; the server remains the authority on validity.
    await expect(modal.getByText("https://app.example.com/*")).toBeVisible();

    await modal.getByTestId("redirectUri").fill(TEST_REDIRECT_URI);
    await getSendButton(page).click();

    await expect
      .poll(() => requestUrl, { timeout: 10_000 })
      .toContain(`client_id=${TEST_CLIENT_ID}`);
    expect(requestUrl).toContain(
      `redirect_uri=${encodeURIComponent(TEST_REDIRECT_URI)}`,
    );
    expect(requestMethod).toBe("PUT");
    expect(requestBody).toEqual(["UPDATE_PASSWORD"]);
    await assertNotificationMessage(page, "Email sent to user.");
  });

  test("surfaces the backend error for unregistered redirect URIs", async ({
    page,
  }) => {
    await using testBed = await createTestBed({
      clients: [testClient],
      users: [resetUser],
    });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    const modal = await openCredentialResetDialog(page);
    await selectRequiredAction(page, modal);
    await selectTestClient(page, modal);

    // Well-formed but not registered for the client: client-side checks
    // pass, the real backend answers 400 before any email is attempted.
    await modal
      .getByTestId("redirectUri")
      .fill("https://evil.example.com/callback");
    const sendButton = getSendButton(page);
    await expect(sendButton).toBeEnabled();
    await sendButton.click();

    await expect(page.getByTestId("last-alert")).toContainText(
      "Invalid redirect uri",
      { timeout: 15_000 },
    );
  });

  test("handles redirect URI edge cases", async ({ page }) => {
    await using testBed = await createTestBed({
      clients: [testClient],
      users: [resetUser],
    });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    const modal = await openCredentialResetDialog(page);
    await selectRequiredAction(page, modal);
    await selectTestClient(page, modal);
    const sendButton = getSendButton(page);

    // Whitespace-only counts as empty and stays sendable.
    await modal.getByTestId("redirectUri").fill("   ");
    await expect(sendButton).toBeEnabled();

    // Root-relative URIs are accepted client-side.
    await modal.getByTestId("redirectUri").fill("/callback");
    await expect(sendButton).toBeEnabled();

    // Removing the client re-disables sending while a URI is set.
    await modal.getByRole("button", { name: "Remove", exact: true }).click();
    await expect(sendButton).toBeDisabled();
  });

  test("sends without client params when fields are empty", async ({
    page,
  }) => {
    await using testBed = await createTestBed({ users: [resetUser] });
    const user = await findResetUser(testBed.realm);

    await login(page, {
      to: toUser({ realm: testBed.realm, id: user.id!, tab: "credentials" }),
    });

    let requestUrl = "";
    await page.route("**/execute-actions-email*", async (route) => {
      requestUrl = route.request().url();
      await route.fulfill({ status: 204 });
    });

    const modal = await openCredentialResetDialog(page);
    await selectRequiredAction(page, modal);

    await getSendButton(page).click();

    await expect
      .poll(() => requestUrl, { timeout: 10_000 })
      .toContain("execute-actions-email");
    expect(requestUrl).not.toContain("client_id");
    expect(requestUrl).not.toContain("redirect_uri");
    await assertNotificationMessage(page, "Email sent to user.");
  });
});
