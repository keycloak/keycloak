import React, { useContext } from "react";
import {
  Page,
  PageHeader,
  PageHeaderTools,
  PageHeaderToolsItem,
  PageSection,
  FormGroup,
  Form,
  TextInput,
} from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { HelpItem } from "../components/help-enabler/HelpItem";
import {
  Help,
  HelpContext,
  HelpHeader,
} from "../components/help-enabler/HelpHeader";

export default {
  title: "Help System Example",
  component: HelpHeader,
} as Meta;

export const HelpSystem = () => (
  <Help>
    <HelpSystemTest />
  </Help>
);

export const HelpItems = () => (
  <HelpItem
    helpText="This explains the related field"
    forLabel="Field label"
    forID="storybook-example-id"
  />
);

export const FormFieldHelp = () => (
  <Form isHorizontal>
    <FormGroup
      label="Label"
      labelIcon={
        <HelpItem
          helpText="This explains the related field"
          forLabel="Field label"
          forID="storybook-form-help"
        />
      }
      fieldId="storybook-form-help"
    >
      <TextInput isRequired type="text" id="storybook-form-help"></TextInput>
    </FormGroup>
  </Form>
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
      <PageSection>Help system is {enabled ? "enabled" : "not on"}</PageSection>
      <PageSection variant="light">
        <FormFieldHelp />
      </PageSection>
    </Page>
  );
};
