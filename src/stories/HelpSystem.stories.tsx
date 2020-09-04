import React, { useContext } from "react";
import {
  Page,
  PageHeader,
  PageHeaderTools,
  PageHeaderToolsItem,
} from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import {
  Help,
  HelpContext,
  HelpHeader,
} from "../components/help-enabler/HelpHeader";

export default {
  title: "Help System Example",
  component: HelpHeader,
} as Meta;

export const HelpSystem = () => {
  return (
    <Help>
      <HelpSystemTest />
    </Help>
  );
};

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
