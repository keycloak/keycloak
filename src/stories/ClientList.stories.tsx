import React from "react";
import { Meta } from "@storybook/react";

import { ClientList } from "../clients/ClientList";
import clientMock from "../clients/__tests__/mock-clients.json";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "Client List",
  component: ClientList,
} as Meta;

export const ClientListExample = () => (
  <MockAdminClient>
    <ClientList
      clients={clientMock}
      baseUrl="http://test.nl/"
      refresh={() => {}}
    />
  </MockAdminClient>
);
