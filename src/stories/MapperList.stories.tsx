import React from "react";
import { Meta } from "@storybook/react";

import serverInfo from "../context/server-info/__tests__/mock.json";
import clientScopeMock from "../client-scopes/__tests__/mock-client-scope.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";
import { MapperList } from "../client-scopes/details/MapperList";

export default {
  title: "Mapper List",
  component: MapperList,
} as Meta;

export const MapperListExample = () => (
  <ServerInfoContext.Provider value={serverInfo}>
    <MapperList clientScope={clientScopeMock} refresh={() => {}} />
  </ServerInfoContext.Provider>
);
