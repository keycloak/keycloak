import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { test, expect } from "@playwright/test";

import { login } from "../utils/login.ts";
import { clickRowKebabItem } from "../utils/table.ts";

test.describe("Event hooks", () => {
    test("should preview the pull consume url when creating a pull target", async ({
        page,
    }) => {
        await login(page, { to: { pathname: "/master/events/hooks/targets" } });

        await expect(
            page.getByRole("button", { name: "Create target" }),
        ).toBeVisible();

        await page.getByRole("button", { name: "Create target" }).click();
        await page.getByLabel("Provider").click();
        await page.getByRole("option", { name: "pull" }).click();

        await expect(page.getByText(/\/admin\/realms\/master\/event-hooks\/\{targetId\}\/consume/)).toBeVisible();
    });

    test("should allow testing a target from the UI without creating persisted logs", async ({
        page,
    }) => {
        let requestCount = 0;
        const server = createServer(
            (_request: IncomingMessage, response: ServerResponse) => {
                requestCount++;
                response.writeHead(200, { "Content-Type": "text/plain" });
                response.end("ok");
            },
        );

        await new Promise<void>((resolve) => server.listen(9911, "127.0.0.1", resolve));

        try {
            await login(page, { to: { pathname: "/master/events/hooks/targets" } });

            await expect(
                page.getByRole("button", { name: "Create target" }),
            ).toBeVisible();

            await page.getByRole("button", { name: "Create target" }).click();
            await page.getByLabel("Name").fill("playwright-hook");
            await page.getByLabel("URL").fill("http://127.0.0.1:9911/hook");
            await page.getByRole("button", { name: "Save" }).click();

            await expect(page.getByText("Event hook target created successfully.")).toBeVisible();
            await expect(page.getByRole("row", { name: /playwright-hook/ })).toBeVisible();

            await clickRowKebabItem(page, "playwright-hook", "Test");
            await expect(page.getByText("Event hook target test succeeded.")).toBeVisible();
            await expect.poll(() => requestCount).toBe(1);

            await page.getByRole("tab", { name: "Logs" }).click();
            await expect(page.getByText("No event hook logs.")).toBeVisible();
        } finally {
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve())),
            );
        }
    });
});