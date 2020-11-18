import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { RolesForm } from "../realm-roles/RealmRoleDetails";

export default {
  title: "Role details tab",
  component: RolesForm,
} as Meta;

export const RoleDetailsExample = () => {
  return (
    <Page>
      <RolesForm />
    </Page>
  );
};
