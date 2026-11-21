import { expect, test } from "@playwright/test";

import { login } from "./utils/login.ts";

test.describe("DPoP-bound console tokens", () => {
  test("Admin REST requests send a DPoP proof", async ({ page }) => {
    const adminRequest = page.waitForRequest((request) =>
      request.url().includes("/admin/realms/"),
    );

    await login(page);

    const request = await adminRequest;
    expect(request.headers()["authorization"]).toMatch(/^DPoP /);
    expect(request.headers()["dpop"]).toBeTruthy();
  });
});
