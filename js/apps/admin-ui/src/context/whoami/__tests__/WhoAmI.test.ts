/**
 * @vitest-environment jsdom
 */
import type WhoAmIRepresentation from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import { expect, test } from "vitest";
import { WhoAmI } from "../WhoAmI";
import whoamiMock from "./mock-whoami.json";

test("returns display name", () => {
  const whoami = new WhoAmI(whoamiMock as WhoAmIRepresentation);
  expect(whoami.getDisplayName()).toEqual("Stan Silvert");
});

test("can not create realm", () => {
  const whoami = new WhoAmI(whoamiMock as WhoAmIRepresentation);
  expect(whoami.canCreateRealm()).toEqual(false);
});

test("getRealmAccess", () => {
  const whoami = new WhoAmI(whoamiMock as WhoAmIRepresentation);
  expect(Object.keys(whoami.getRealmAccess()).length).toEqual(3);
  expect(whoami.getRealmAccess()["master"].length).toEqual(18);
});

//TODO: When we have easy access to i18n, create test for setting locale.
//      Tested manually and it does work.
