import React from "react";
import { Meta } from "@storybook/react";
import { Page } from "@patternfly/react-core";
import { UserFederationLdapSettingsTab } from "../user-federation/UserFederationLdapSettingsTab";

export default {
  title: "User Federation LDAP Settings Tab",
  component: UserFederationLdapSettingsTab,
} as Meta;

export const view = () => {
  return (
    <Page>
      <UserFederationLdapSettingsTab />
    </Page>
  );
};
