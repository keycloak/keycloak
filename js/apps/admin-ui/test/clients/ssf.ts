import { expect, type Page } from "@playwright/test";
import { toClient } from "../../src/clients/routes/Client.tsx";
import {
  type SsfClientTab,
  toSsfClientTab,
} from "../../src/clients/routes/ClientSsfTab.tsx";
import { login, navigateTo } from "../utils/login.ts";

type SsfTabOptions = {
  realm: string;
  clientUuid: string;
  tab: SsfClientTab;
};

const SSF_TAB_TEST_IDS: Record<SsfClientTab, string> = {
  receiver: "ssfReceiverTab",
  stream: "ssfStreamTab",
  subjects: "ssfSubjectsTab",
  "event-search": "ssfEventSearchTab",
  "emit-events": "ssfEmitEventsTab",
};

const SSF_STREAM_AUDIENCE_FIELD_TEST_ID = `attributes.${"ssf.streamAudience".replaceAll(".", "🍺")}`;

function getSsfSubTab(page: Page, tab: SsfClientTab) {
  return page.getByTestId(SSF_TAB_TEST_IDS[tab]);
}

function getSsfPushDeliveryMethodCheckbox(page: Page) {
  return page.getByTestId("ssfAllowedDeliveryMethods.push");
}

function getSsfPollDeliveryMethodCheckbox(page: Page) {
  return page.getByTestId("ssfAllowedDeliveryMethods.poll");
}

function getSsfAudienceField(page: Page) {
  return page.getByTestId(SSF_STREAM_AUDIENCE_FIELD_TEST_ID);
}

export async function loginToSsfTab(page: Page, options: SsfTabOptions) {
  await login(page, {
    to: toSsfClientTab({
      realm: options.realm,
      clientId: options.clientUuid,
      tab: options.tab,
    }),
  });
  await assertSsfTabVisible(page);
}

export async function navigateToSsfTab(page: Page, options: SsfTabOptions) {
  await navigateTo(
    page,
    toSsfClientTab({
      realm: options.realm,
      clientId: options.clientUuid,
      tab: options.tab,
    }),
  );
  await assertSsfTabVisible(page);
}

export async function navigateToClientSettings(
  page: Page,
  realm: string,
  clientUuid: string,
) {
  await navigateTo(
    page,
    toClient({ realm, clientId: clientUuid, tab: "settings" }),
  );
}

export async function assertSsfTabVisible(page: Page) {
  await expect(page.getByTestId("ssfTab")).toBeVisible({ timeout: 15_000 });
}

export async function assertSsfTabHidden(page: Page) {
  await expect(page.getByTestId("ssfTab")).toHaveCount(0);
}

export async function assertSsfNavigationTabsVisible(page: Page) {
  await assertSsfTabVisible(page);
  await expect(page.getByTestId("ssfReceiverTab")).toBeVisible();
  await expect(page.getByTestId("ssfStreamTab")).toBeVisible();
  await expect(page.getByTestId("ssfSubjectsTab")).toBeVisible();
  await expect(page.getByTestId("ssfEventSearchTab")).toBeVisible();
  await expect(page.getByTestId("ssfEmitEventsTab")).toBeVisible();
}

export async function openSsfSubTab(page: Page, tab: SsfClientTab) {
  await getSsfSubTab(page, tab).click();
}

export async function assertClientSettingsLoaded(page: Page) {
  await expect(page.getByTestId("clientId")).toBeVisible();
}

export async function assertSsfReceiverSaveButtonVisible(page: Page) {
  await expect(page.getByTestId("ssfReceiverSave")).toBeVisible();
}

export async function assertSsfStreamEmptyStateVisible(page: Page) {
  await expect(page.getByTestId("empty-state")).toBeVisible();
}

export async function assertSsfPendingLookupVisible(page: Page) {
  await expect(page.getByTestId("ssfPendingLookup")).toBeVisible();
}

export async function assertSsfSubjectTypeVisible(page: Page) {
  await expect(page.getByTestId("ssfSubjectType")).toBeVisible();
}

export async function openCreateSsfStreamForm(page: Page) {
  await page.getByRole("button", { name: "Create stream" }).click();
}

function getCreateSsfStreamSubmitButton(page: Page) {
  return page.getByTestId("ssfCreateStreamSubmit");
}

export async function assertCreateSsfStreamEndpointVisible(page: Page) {
  await expect(page.getByTestId("ssfCreateStreamEndpointUrl")).toBeVisible();
}

export async function fillCreateSsfStreamEndpoint(page: Page, value: string) {
  await page.getByTestId("ssfCreateStreamEndpointUrl").fill(value);
}

export async function fillCreateSsfStreamDescription(
  page: Page,
  value: string,
) {
  await page.getByTestId("ssfCreateStreamDescription").fill(value);
}

export async function assertCreateSsfStreamSubmitDisabled(page: Page) {
  await expect(getCreateSsfStreamSubmitButton(page)).toBeDisabled();
}

export async function assertCreateSsfStreamSubmitEnabled(page: Page) {
  await expect(getCreateSsfStreamSubmitButton(page)).toBeEnabled();
}

export async function assertCreateSsfStreamEndpointErrorVisible(page: Page) {
  await expect(page.getByTestId("endpointUrl-helper")).toBeVisible();
}

export async function assertCreateSsfStreamEndpointErrorHidden(page: Page) {
  await expect(page.getByTestId("endpointUrl-helper")).toBeHidden();
}

export async function submitCreateSsfStream(page: Page) {
  await getCreateSsfStreamSubmitButton(page).click();
}

export async function assertRegisteredSsfStreamView(page: Page) {
  await expect(page.getByTestId("ssfRefresh")).toBeVisible();
  await expect(page.getByTestId("empty-state")).toBeHidden();
}

export async function fillSsfSubjectValue(page: Page, value: string) {
  await page.getByTestId("ssfSubjectValue").fill(value);
}

export async function checkSsfSubject(page: Page) {
  await page.getByTestId("ssfSubjectCheck").click();
}

export async function addSsfSubject(page: Page) {
  await page.getByTestId("ssfSubjectAdd").click();
}

export async function ignoreSsfSubject(page: Page) {
  await page.getByTestId("ssfSubjectIgnore").click();
}

export async function removeSsfSubject(page: Page) {
  await page.getByTestId("ssfSubjectRemove").click();
}

export async function assertSsfSubjectValueError(page: Page, message: string) {
  await expect(page.getByTestId("ssfSubjectValueError")).toHaveText(message);
}

export async function assertSsfSubjectStatus(page: Page, message: string) {
  await expect(page.getByTestId("ssfSubjectStatus")).toHaveText(message);
}

export async function assertSsfAudienceValue(page: Page, value: string) {
  await expect(getSsfAudienceField(page)).toHaveValue(value);
}

export async function fillSsfAudience(page: Page, value: string) {
  await getSsfAudienceField(page).fill(value);
}

export async function clickSsfReceiverRevert(page: Page) {
  await page.getByTestId("ssfReceiverRevert").click();
}

export async function clickSsfReceiverSave(page: Page) {
  await page.getByTestId("ssfReceiverSave").click();
}

export async function assertSsfPushAndPollDeliveryMethodsChecked(page: Page) {
  await expect(getSsfPushDeliveryMethodCheckbox(page)).toBeChecked();
  await expect(getSsfPollDeliveryMethodCheckbox(page)).toBeChecked();
}

export async function disableSsfPollDeliveryMethod(page: Page) {
  await getSsfPollDeliveryMethodCheckbox(page).click();
}

export async function assertSsfPushDeliveryMethodChecked(page: Page) {
  await expect(getSsfPushDeliveryMethodCheckbox(page)).toBeChecked();
}

export async function assertSsfPollDeliveryMethodUnchecked(page: Page) {
  await expect(getSsfPollDeliveryMethodCheckbox(page)).not.toBeChecked();
}
