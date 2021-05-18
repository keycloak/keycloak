import React from "react";
import type { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationLdapWizard } from "../user-federation/UserFederationLdapWizard";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "User Federation LDAP Wizard",
  component: UserFederationLdapWizard,
} as Meta;

export const view = () => {
  return (
    <Page style={{ height: "80vh" }}>
      <MockAdminClient>
        <UserFederationLdapWizard />
      </MockAdminClient>{" "}
    </Page>
  );
};
