import React from "react";
import { Meta, Story } from "@storybook/react";
import { Page, SelectOption } from "@patternfly/react-core";
import {
  ViewHeader,
  ViewHeaderProps,
} from "../components/view-header/ViewHeader";

export default {
  title: "View Header",
  component: ViewHeader,
} as Meta;

const Template: Story<ViewHeaderProps> = (args) => (
  <Page>
    <ViewHeader {...args} />
  </Page>
);

export const Extended = Template.bind({});
Extended.args = {
  titleKey: "This is the title",
  badge: "badge",
  subKey: "This is the description.",
  subKeyLinkProps: {
    title: "More information",
    href: "http://google.com",
  },
  selectItems: [
    <SelectOption key="first" value="first-item">
      First item
    </SelectOption>,
    <SelectOption key="second" value="second-item">
      Second item
    </SelectOption>,
  ],
};

export const Simple = Template.bind({});
Simple.args = {
  titleKey: "Title simple",
  subKey: "Some lengthy description about what this is about.",
};
