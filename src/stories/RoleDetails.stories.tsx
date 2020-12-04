import React from "react";
import { Page } from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { MockAdminClient } from "./MockAdminClient";
import { RealmRolesForm } from "../realm-roles/RealmRoleForm";

import rolesMock from "../realm-roles/__tests__/mock-roles.json";
import { MemoryRouter, Route } from "react-router-dom";

export default {
  title: "Role details tab",
  component: RealmRolesForm,
} as Meta;

export const RoleDetailsExample = () => {
  return (
    <Page>
      <MockAdminClient mock={{ roles: { findOneById: () => rolesMock[0] } }}>
        <MemoryRouter initialEntries={["/roles/1"]}>
          <Route path="/roles/:id">
            <RealmRolesForm />
          </Route>
        </MemoryRouter>
      </MockAdminClient>
    </Page>
  );
};

export const RoleDetailsNew = () => {
  return (
    <Page>
      <MockAdminClient>
        <RealmRolesForm />
      </MockAdminClient>
    </Page>
  );
};
