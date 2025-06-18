import { Page, expect } from "@playwright/test";
import {
  assertSelectValue,
  selectItem,
  switchOff,
  switchOn,
} from "../utils/form";
import { confirmModal } from "../utils/modal";
import { clickRowKebabItem } from "../utils/table";

export async function goToAdvancedTab(page: Page) {
  await page.getByTestId("advancedTab").click();
}

export async function expandClusterNode(page: Page) {
  await page.getByRole("button", { name: "Registered cluster nodes" }).click();
}

export async function registerNodeManually(page: Page, host: string) {
  await page.getByTestId("no-nodes-registered-empty-action").click();
  await page.getByTestId("node").fill(host);
  await page.getByRole("button", { name: "Save" }).click();
}

export async function assertTestClusterAvailability(
  page: Page,
  expected: boolean = true,
) {
  const button = page.getByTestId("test-cluster-availability");
  if (expected) {
    await expect(button).toBeEnabled();
  } else {
    await expect(button).toBeDisabled();
  }
}

export async function deleteClusterNode(page: Page, host: string) {
  await clickRowKebabItem(page, host, "Delete");
  await confirmModal(page);
}

function getAccessTokenSignatureAlgorithm(page: Page) {
  return page.locator("#attributes\\.access🍺token🍺signed🍺response🍺alg");
}

export async function selectAccessTokenSignatureAlgorithm(
  page: Page,
  algorithm: string,
) {
  await selectItem(page, getAccessTokenSignatureAlgorithm(page), algorithm);
}

export async function assertAccessTokenSignatureAlgorithm(
  page: Page,
  value: string,
) {
  await assertSelectValue(getAccessTokenSignatureAlgorithm(page), value);
}

export async function saveFineGrain(page: Page) {
  await page.getByTestId("fineGrainSave").click();
}

export async function revertFineGrain(page: Page) {
  await page.getByTestId("fineGrainRevert").click();
}

const excludeSessionStateSwitch =
  "#excludeSessionStateFromAuthenticationResponse-switch";

export async function switchOffExcludeSessionStateSwitch(page: Page) {
  await switchOff(page, excludeSessionStateSwitch);
}

export async function assertOnExcludeSessionStateSwitch(page: Page) {
  await expect(page.locator(excludeSessionStateSwitch)).toBeChecked();
}

export async function clickAllCompatibilitySwitch(page: Page) {
  await switchOn(page, excludeSessionStateSwitch);
  await switchOff(page, "#useRefreshTokens");
  await switchOn(page, "#useRefreshTokenForClientCredentialsGrant");
  await switchOn(page, "#useLowerCaseBearerType");
}

export async function saveCompatibility(page: Page) {
  await page.getByTestId("OIDCCompatabilitySave").click();
}

export async function revertCompatibility(page: Page) {
  await page.getByTestId("OIDCCompatabilityRevert").click();
}

export async function assertTokenLifespanClientOfflineSessionMaxVisible(
  page: Page,
  visible: boolean,
) {
  const locator = page.getByTestId("token-lifespan-clientOfflineSessionMax");
  if (visible) {
    await expect(locator).toBeVisible();
  } else {
    await expect(locator).toBeHidden();
  }
}

const oAuthMutualSwitch =
  "#attributes\\.tls🍺client🍺certificate🍺bound🍺access🍺tokens";
const pushedAuthorizationRequestRequiredSwitch =
  "#attributes\\.require🍺pushed🍺authorization🍺requests";

export async function clickAdvancedSwitches(page: Page, toggle = true) {
  if (toggle) {
    await switchOn(page, oAuthMutualSwitch);
    await switchOn(page, pushedAuthorizationRequestRequiredSwitch);
  } else {
    await switchOff(page, oAuthMutualSwitch);
    await switchOff(page, pushedAuthorizationRequestRequiredSwitch);
  }
}

export async function assertAdvancedSwitchesOn(page: Page) {
  await expect(page.locator(oAuthMutualSwitch)).toBeChecked();
  await expect(
    page.locator(pushedAuthorizationRequestRequiredSwitch),
  ).toBeChecked();
}

export async function saveAdvanced(page: Page) {
  await page.getByTestId("OIDCAdvancedSave").click();
}

export async function revertAdvanced(page: Page) {
  await page.getByTestId("OIDCAdvancedRevert").click();
}

function getBrowserFlowInput(page: Page) {
  return page.locator("#authenticationFlowBindingOverrides\\.browser");
}

function getDirectFlowInput(page: Page) {
  return page.locator("#authenticationFlowBindingOverrides\\.direct_grant");
}

export async function selectBrowserFlowInput(page: Page, value: string) {
  await selectItem(page, getBrowserFlowInput(page), value);
}

export async function selectDirectGrantInput(page: Page, value: string) {
  await selectItem(page, getDirectFlowInput(page), value);
}

export async function assertBrowserFlowInput(page: Page, value: string) {
  await assertSelectValue(getBrowserFlowInput(page), value);
}

export async function assertDirectGrantInput(page: Page, value: string) {
  await assertSelectValue(getDirectFlowInput(page), value);
}

export async function saveAuthFlowOverride(page: Page) {
  await page.getByTestId("OIDCAuthFlowOverrideSave").click();
}

export async function revertAuthFlowOverride(page: Page) {
  await page.getByTestId("OIDCAuthFlowOverrideRevert").click();
}
