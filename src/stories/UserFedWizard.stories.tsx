import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationKerberosWizard } from "../user-federation/UserFederationKerberosWizard";

export default {
  title: "User Federation Wizard",
  component: UserFederationKerberosWizard,
} as Meta;

export const view = () => {
  return (
    <Page>
      <UserFederationKerberosWizard />
    </Page>
  );
};
