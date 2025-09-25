import { test } from "@playwright/test";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertNotificationMessage } from "../utils/masthead.ts";
import { goToIdentityProviders } from "../utils/sidebar.ts";
import { clickTableRowItem } from "../utils/table.ts";
import { clickSaveButton, createKubernetesProvider } from "./main.ts";

test.beforeEach(async ({ page }) => {
  await login(page);
  await goToIdentityProviders(page);
});

test.afterAll(() => adminClient.deleteIdentityProvider("kubernetes"));

test.describe.serial("Kubernetes identity provider test", () => {
  test("should create a Kubernetes provider", async ({ page }) => {
    await createKubernetesProvider(
      page,
      "kubernetes",
      "https://kubernetes.myorg.com/openid/v1/jwks",
    );

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "kubernetes");

    await page
      .getByTestId("config.jwksUrl")
      .fill("https://kubernetes.myorg2.com/openid/v1/jwks");

    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
  });
});
