import { Page } from "@playwright/test";
import path from "path";
import { fileURLToPath } from "url";

export async function chooseFile(page: Page, file: string) {
  const fileChooserPromise = page.waitForEvent("filechooser");
  await page.getByText("Browse...").click();
  const fileChooser = await fileChooserPromise;

  const fileName = fileURLToPath(import.meta.url);
  const dirName = path.dirname(fileName);
  const pathName = path.resolve(dirName, file);

  await fileChooser.setFiles(pathName);
}
