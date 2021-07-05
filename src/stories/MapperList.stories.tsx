import React from "react";
import type { Meta } from "@storybook/react";
import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import serverInfo from "../context/server-info/__tests__/mock.json";
import clientScopeMock from "../client-scopes/__tests__/mock-client-scope.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";
import { MapperList } from "../client-scopes/details/MapperList";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "Mapper List",
  component: MapperList,
} as Meta;

export const MapperListExample = () => (
  <ServerInfoContext.Provider
    value={serverInfo as unknown as ServerInfoRepresentation}
  >
    <MockAdminClient>
      <MapperList clientScope={clientScopeMock} refresh={() => {}} />
    </MockAdminClient>
  </ServerInfoContext.Provider>
);
