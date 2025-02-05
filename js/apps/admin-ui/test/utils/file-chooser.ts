import { Page } from "@playwright/test";
import path from "path";

export async function chooseFile(page: Page, file: string) {
  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("Browse...").click();
  const fileChooser = await fileChooserPromise;
  await fileChooser.setFiles(
    new URL(path.join(path.dirname(import.meta.url), file)).pathname,
  );
}
