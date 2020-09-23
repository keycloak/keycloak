import React from "react";
import { MemoryRouter } from "react-router-dom";
import { render } from "@testing-library/react";

import clientMock from "./mock-clients.json";
import { ClientList } from "../ClientList";

test("renders ClientList", () => {
  const container = render(
    <MemoryRouter>
      <ClientList clients={clientMock} baseUrl="http://blog.nerdin.ch" />
    </MemoryRouter>
  );
  expect(container).toMatchSnapshot();
});
