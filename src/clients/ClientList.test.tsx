import React from "react";
import { render } from "@testing-library/react";

import clientMock from "./mock-clients.json";
import { ClientList } from "./ClientList";

test("renders ClientList", () => {
  const { getByText } = render(
    <ClientList clients={clientMock} baseUrl="http://blog.nerdin.ch" />
  );
  const headerElement = getByText(/Client ID/i);
  expect(headerElement).toBeInTheDocument();
});
