import React from "react";
import type { Meta } from "@storybook/react";
import { MockAdminClient } from "./MockAdminClient";
import rolesMock from "../realm-roles/__tests__/mock-roles.json";
import { RealmRoleTabs } from "../realm-roles/RealmRoleTabs";

export default {
  title: "Roles tabs",
  component: RealmRoleTabs,
} as Meta;

export const RolesTabsExample = () => {
  return (
    <MockAdminClient mock={{ roles: { findOneById: () => rolesMock[0] } }}>
      <RealmRoleTabs />
    </MockAdminClient>
  );
};
