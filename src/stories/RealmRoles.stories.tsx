import React from "react";
import { Meta } from "@storybook/react";
import { RolesList } from "../realm-roles/RoleList";
import rolesMock from "../realm-roles/__tests__/mock-roles.json";

export default {
  title: "Roles List",
  component: RolesList,
} as Meta;

export const RolesListExample = () => (
  <RolesList roles={rolesMock} refresh={() => {}} />
);
