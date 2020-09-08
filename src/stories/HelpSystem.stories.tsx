import React, { useContext } from "react";
import {
  Page,
  PageHeader,
  PageHeaderTools,
  PageHeaderToolsItem,
} from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { HelpItem } from "../components/help-enabler/HelpItem";
import {
  Help,
  HelpContext,
  HelpHeader,
} from "../components/help-enabler/HelpHeader";
import { I18nextProvider } from "react-i18next";
import { i18n } from "../i18n";

export default {
  title: "Help System Example",
  component: HelpHeader,
} as Meta;

export const HelpSystem = () => (
  <Help>
    <HelpSystemTest />
  </Help>
);

export const HelpItemz = () => (
  <I18nextProvider i18n={i18n}>
    <HelpItem item="storybook" />
  </I18nextProvider>
);

const HelpSystemTest = () => {
  const { enabled } = useContext(HelpContext);
  return (
    <Page
      header={
        <PageHeader
          headerTools={
            <PageHeaderTools>
              <PageHeaderToolsItem>
                <HelpHeader />
              </PageHeaderToolsItem>
              <PageHeaderToolsItem>dummy user...</PageHeaderToolsItem>
            </PageHeaderTools>
          }
        />
      }
    >
      Help system is {enabled ? "enabled" : "not on, guess you don't need help"}
    </Page>
  );
};
