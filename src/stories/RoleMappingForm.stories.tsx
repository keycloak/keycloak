import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";

import serverInfo from "../context/server-info/__tests__/mock.json";
import roles from "../realm-roles/__tests__/mock-roles.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";

import { RoleMappingForm } from "../client-scopes/add/RoleMappingForm";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { HttpClient } from "../context/http-service/http-client";

export default {
  title: "Role Mapping Form",
  component: RoleMappingForm,
} as Meta;

export const RoleMappingFormExample = () => (
  <ServerInfoContext.Provider value={serverInfo}>
    <HttpClientContext.Provider
      value={
        ({
          doGet: () => {
            return { data: roles };
          },
        } as unknown) as HttpClient
      }
    >
      <Page>
        <RoleMappingForm clientScopeId="dummy" />
      </Page>
    </HttpClientContext.Provider>
  </ServerInfoContext.Provider>
);
