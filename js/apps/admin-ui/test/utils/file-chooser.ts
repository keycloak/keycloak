import type { Locator, Page } from "@playwright/test";
import path from "node:path";
import { fileURLToPath } from "node:url";

export async function chooseFile(page: Page, file: string) {
  const locator = page.getByText("Browse...");
  await chooseFileByLocator(page, file, locator);
}

export async function chooseFileByLocator(
  page: Page,
  file: string,
  locator: Locator,
) {
  const fileChooserPromise = page.waitForEvent("filechooser");
  await locator.click();
  const fileChooser = await fileChooserPromise;

  const fileName = fileURLToPath(import.meta.url);
  const dirName = path.dirname(fileName);
  const pathName = path.resolve(dirName, file);

  await fileChooser.setFiles(pathName);
}
