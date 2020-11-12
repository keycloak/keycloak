import React from "react";
import { MemoryRouter } from "react-router-dom";
import { render } from "@testing-library/react";
import KeycloakAdminClient from "keycloak-admin";

import clientMock from "./mock-clients.json";
import { ClientList } from "../ClientList";
import { AdminClient } from "../../context/auth/AdminClient";

test("renders ClientList", () => {
  const container = render(
    <MemoryRouter>
      <AdminClient.Provider
        value={
          ({
            setConfig: () => {},
          } as unknown) as KeycloakAdminClient
        }
      >
        <ClientList
          clients={clientMock}
          baseUrl="http://blog.nerdin.ch"
          refresh={() => {}}
        />
      </AdminClient.Provider>
    </MemoryRouter>
  );
  expect(container).toMatchSnapshot();
});
