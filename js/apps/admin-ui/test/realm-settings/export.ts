import { Page, expect } from "@playwright/test";
import { switchOff, switchOn } from "../utils/form";

export async function assertDialogClosed(page: Page) {
  await expect(page.getByTestId("confirm")).not.toBeVisible();
}

export async function assertWarningMessage(page: Page, toBeVisible = false) {
  if (toBeVisible) {
    await expect(page.getByTestId("warning-message")).toBeVisible();
  } else {
    await expect(page.getByTestId("warning-message")).not.toBeVisible();
  }
}

function getIncludeGroupsAndRoles() {
  return "[data-testid='include-groups-and-roles-check']";
}

export async function toggleIncludeGroupsAndRoles(page: Page, on = true) {
  if (on) {
    await switchOn(page, getIncludeGroupsAndRoles());
  } else {
    await switchOff(page, getIncludeGroupsAndRoles());
  }
}

function getIncludeClients() {
  return "[data-testid='include-clients-check']";
}

export async function toggleIncludeClients(page: Page, on = true) {
  if (on) {
    await switchOn(page, getIncludeClients());
  } else {
    await switchOff(page, getIncludeClients());
  }
}
