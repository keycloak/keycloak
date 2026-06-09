import { expect, test } from "@playwright/test";
import { login } from "../utils/login.ts";

const addSamlProviderUrl =
  "http://localhost:8080/admin/master/console/#/master/identity-providers/saml/add";

test("should enable SAML signature switches by default", async ({ page }) => {
  await login(page);
  await page.goto(addSamlProviderUrl);

  const wantAssertionsSigned = page.getByTestId("config.wantAssertionsSigned");
  await wantAssertionsSigned.waitFor({ state: "attached" });
  await expect(wantAssertionsSigned).toBeChecked();

  const validateSignature = page.getByTestId("config.validateSignature");
  await validateSignature.waitFor({ state: "attached" });
  await expect(validateSignature).toBeChecked();
});
