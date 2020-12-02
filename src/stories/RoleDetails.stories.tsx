import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { RealmRolesForm } from "../realm-roles/RealmRoleForm";

export default {
  title: "Role details tab",
  component: RealmRolesForm,
} as Meta;

export const RoleDetailsExample = () => {
  return (
    <Page>
      <RealmRolesForm />
    </Page>
  );
};
