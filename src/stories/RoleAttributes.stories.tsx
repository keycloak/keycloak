import React from "react";
import { Meta } from "@storybook/react";
import { MockAdminClient } from "./MockAdminClient";
import { MemoryRouter, Route } from "react-router-dom";
import rolesMock from "../realm-roles/__tests__/mock-roles.json";
import { RolesTabs } from "../realm-roles/RealmRoleTabs";

export default {
  title: "Roles tabs",
  component: RolesTabs,
} as Meta;

export const RoleTabsExample = () => {
  return (
    <MockAdminClient mock={{ roles: { findOneById: () => rolesMock[0] } }}>
      <MemoryRouter initialEntries={["/roles/1"]}>
        <Route path="/roles/:id">
          <RolesTabs />
        </Route>
      </MemoryRouter>
    </MockAdminClient>
  );
};
