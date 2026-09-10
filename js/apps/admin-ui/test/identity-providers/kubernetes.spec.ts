import { expect, test } from "@playwright/test";
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
      "https://kubernetes.myorg.com",
    );

    await assertNotificationMessage(
      page,
      "Identity provider successfully created",
    );

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "kubernetes");

    await page
      .getByTestId("config.issuer")
      .fill("https://kubernetes2.myorg.com");

    await clickSaveButton(page);

    await assertNotificationMessage(page, "Provider successfully updated");
  });

  test("should set and persist federated client assertion max expiration", async ({
    page,
  }) => {
    await clickTableRowItem(page, "kubernetes");

    const maxExpInput = page.getByTestId(
      "fed-client-assertion-max-expiration-time-input",
    );
    await maxExpInput.fill("5");

    await clickSaveButton(page);
    await assertNotificationMessage(page, "Provider successfully updated");

    await goToIdentityProviders(page);
    await clickTableRowItem(page, "kubernetes");

    await expect(
      page.getByTestId("fed-client-assertion-max-expiration-time-input"),
    ).toHaveValue("5");
  });
});
