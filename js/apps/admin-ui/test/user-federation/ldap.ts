import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation.js";
import { type Page, expect } from "@playwright/test";
import { selectItem } from "../utils/form.ts";

const filteredConfig = ["vendor", "bindType", "useTruststoreSpi", "editMode"];
export async function fillLdapForm(page: Page, ldap: ComponentRepresentation) {
  await page.getByTestId("name").fill(ldap.name!);

  if (ldap.config?.vendor) {
    await selectItem(page, "#kc-vendor", ldap.config.vendor[0]);
  }

  if (ldap.config?.bindType) {
    await selectItem(page, "#kc-bind-type", ldap.config.bindType[0]);
  }

  if (ldap.config?.truststoreSpiAlways) {
    await selectItem(
      page,
      "#useTruststoreSpi",
      ldap.config.useTruststoreSpi[0],
    );
  }

  if (ldap.config?.editMode) {
    await selectItem(page, "#editMode", ldap.config.editMode[0]);
  }

  for (const key in ldap.config) {
    if (filteredConfig.includes(key)) {
      continue;
    }
    await page
      .getByTestId(`config.${key}.0`)
      .fill(ldap.config[key][0], { force: true });
  }
}

export async function selectEvictionPolicy(page: Page, policy: string) {
  await page.locator("#kc-cache-policy").click();
  await page.getByRole("option", { name: policy }).click();
}

export async function fillEviction(page: Page, evict: [string, string]) {
  await selectItem(page, `#kc-eviction-${evict[0]}`, evict[1]);
}

export async function clickLdapCard(page: Page, name: string) {
  await page.getByRole("link", { name }).click();
}

export async function assertEvictionValues(
  page: Page,
  evictionValues: [string, string][],
) {
  for (const evict of evictionValues) {
    await expect(page.locator(`#kc-eviction-${evict[0]}`)).toHaveText(evict[1]);
  }
}
