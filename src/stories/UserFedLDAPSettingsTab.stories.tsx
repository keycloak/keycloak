import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationLdapSettings } from "../user-federation/UserFederationLdapSettings";

export default {
  title: "User Federation LDAP Settings Tab",
  component: UserFederationLdapSettings,
} as Meta;

export const view = () => {
  return (
    <Page>
      <UserFederationLdapSettings />
    </Page>
  );
};
