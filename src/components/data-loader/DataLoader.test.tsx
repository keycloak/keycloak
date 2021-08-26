import { render, waitFor } from "@testing-library/react";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import React, { FunctionComponent } from "react";
import { HashRouter } from "react-router-dom";
import { AccessContextProvider } from "../../context/access/Access";
import { AdminClient } from "../../context/auth/AdminClient";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { ServerInfoContext } from "../../context/server-info/ServerInfoProvider";
import serverInfo from "../../context/server-info/__tests__/mock.json";
import { WhoAmIContextProvider } from "../../context/whoami/WhoAmI";
import whoamiMock from "../../context/whoami/__tests__/mock-whoami.json";
import { DataLoader } from "./DataLoader";

/**
 * This component provides some mocked default react context so that other components can work in a storybook.
 * In it's simplest form wrap your component like so:
 * @example
 *  <MockAdminClient>
 *    <SomeComponent />
 *  </MockAdminClient>
 * @example <caption>With an endpoint, roles => findOneById</caption>
 *   <MockAdminClient mock={{ roles: { findOneById: () => mockJson } }}>
 *     <<SomeComponent />
 *   </MockAdminClient>
 * @param props mock endpoints to be mocked
 */
export const MockAdminClient: FunctionComponent<{ mock?: object }> = (
  props
) => {
  return (
    <HashRouter>
      <ServerInfoContext.Provider
        value={serverInfo as unknown as ServerInfoRepresentation}
      >
        <AdminClient.Provider
          value={
            {
              ...props.mock,
              keycloak: {},
              whoAmI: { find: () => Promise.resolve(whoamiMock) },
              setConfig: () => {},
            } as unknown as KeycloakAdminClient
          }
        >
          <WhoAmIContextProvider>
            <RealmContext.Provider
              value={{
                realm: "master",
                setRealm: () => {},
                realms: [],
                refresh: () => Promise.resolve(),
              }}
            >
              <AccessContextProvider>{props.children}</AccessContextProvider>
            </RealmContext.Provider>
          </WhoAmIContextProvider>
        </AdminClient.Provider>
      </ServerInfoContext.Provider>
    </HashRouter>
  );
};

describe("DataLoader", () => {
  it("loads the data and renders the result", async () => {
    const loader = () => Promise.resolve(["a", "b"]);
    const { container } = render(
      <MockAdminClient>
        <DataLoader loader={loader}>
          {(result) => (
            <div>
              {result.map((value) => (
                <i key={value}>{value}</i>
              ))}
            </div>
          )}
        </DataLoader>
      </MockAdminClient>
    );

    await waitFor(() => expect(container.textContent).toEqual("ab"));
  });
});
