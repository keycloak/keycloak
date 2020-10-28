import whoamiMock from "./mock-whoami.json";
import { WhoAmI } from "../WhoAmI";

test("returns display name", () => {
  const whoami = new WhoAmI("master", whoamiMock);
  expect(whoami.getDisplayName()).toEqual("Stan Silvert");
});

test("returns correct home realm", () => {
  let whoami = new WhoAmI("myrealm", whoamiMock);
  expect(whoami.getHomeRealm()).toEqual("myrealm");
  whoami = new WhoAmI(undefined, whoamiMock);
  expect(whoami.getHomeRealm()).toEqual("master");
});

test("can not create realm", () => {
  const whoami = new WhoAmI("master", whoamiMock);
  expect(whoami.canCreateRealm()).toEqual(false);
});

test("getRealmAccess", () => {
  const whoami = new WhoAmI("master", whoamiMock);
  expect(Object.keys(whoami.getRealmAccess()).length).toEqual(3);
  expect(whoami.getRealmAccess()["master"].length).toEqual(18);
});

//TODO: When we have easy access to i18n, create test for setting locale.
//      Tested manually and it does work.
