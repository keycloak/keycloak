import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";

import serverInfo from "../context/server-info/__tests__/mock.json";
import roles from "../realm-roles/__tests__/mock-roles.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";

import { RoleMappingForm } from "../client-scopes/add/RoleMappingForm";
import { AdminClient } from "../context/auth/AdminClient";
import KeycloakAdminClient from "keycloak-admin";

export default {
  title: "Role Mapping Form",
  component: RoleMappingForm,
} as Meta;

export const RoleMappingFormExample = () => (
  <ServerInfoContext.Provider value={serverInfo}>
    <AdminClient.Provider
      value={
        ({
          setConfig: () => {},
          roles: {
            find: () => {
              return roles;
            },
          },
          clients: {
            find: () => roles,
          },
        } as unknown) as KeycloakAdminClient
      }
    >
      <Page>
        <RoleMappingForm clientScopeId="dummy" />
      </Page>
    </AdminClient.Provider>
  </ServerInfoContext.Provider>
);
