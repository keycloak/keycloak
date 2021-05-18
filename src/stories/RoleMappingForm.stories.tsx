import React from "react";
import type { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import type { ServerInfoRepresentation } from "keycloak-admin/lib/defs/serverInfoRepesentation";

import serverInfo from "../context/server-info/__tests__/mock.json";
import roles from "../realm-roles/__tests__/mock-roles.json";
import clients from "../clients/__tests__/mock-clients.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";

import { RoleMappingForm } from "../client-scopes/add/RoleMappingForm";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "Role Mapping Form",
  component: RoleMappingForm,
} as Meta;

export const RoleMappingFormExample = () => (
  <ServerInfoContext.Provider
    value={(serverInfo as unknown) as ServerInfoRepresentation}
  >
    <MockAdminClient
      mock={{
        roles: {
          find: () => roles,
        },
        clients: {
          find: () => clients,
          listRoles: () => roles,
        },
      }}
    >
      <Page>
        <RoleMappingForm />
      </Page>
    </MockAdminClient>
  </ServerInfoContext.Provider>
);
