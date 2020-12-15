import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationLdapWizard } from "../user-federation/UserFederationLdapWizard";

export default {
  title: "User Federation LDAP Wizard",
  component: UserFederationLdapWizard,
} as Meta;

export const view = () => {
  return (
    <Page>
      <UserFederationLdapWizard />
    </Page>
  );
};
