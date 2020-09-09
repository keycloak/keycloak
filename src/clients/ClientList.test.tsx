import React from "react";
import { render } from "@testing-library/react";

import clientMock from "./mock-clients.json";
import { ClientList } from "./ClientList";
import { I18nextProvider } from "react-i18next";
import { i18n } from "../i18n";

test("renders ClientList", () => {
  const { getByText } = render(
    <I18nextProvider i18n={i18n}>
      <ClientList clients={clientMock} baseUrl="http://blog.nerdin.ch" />
    </I18nextProvider>
  );
  const headerElement = getByText(/Client ID/i);
  expect(headerElement).toBeInTheDocument();
});
