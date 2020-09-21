import React from "react";
import { Meta, Story } from "@storybook/react";
import { ExternalLink } from "../components/external-link/ExternalLink";

export default {
  title: "External link",
  component: ExternalLink,
} as Meta;

const Template: Story<React.HTMLProps<HTMLAnchorElement>> = (args) => (
  <ExternalLink {...args} />
);

export const WithTitle = Template.bind({});
WithTitle.args = {
  title: "With title",
  href: "http://test.nl",
};

export const WithoutTitle = Template.bind({});
WithoutTitle.args = {
  href: "http://some-other-link.nl/super",
};

export const ApplicationLink = Template.bind({});
ApplicationLink.args = {
  title: "Application link",
  href: "/application/main",
};

export const DisabledLink = Template.bind({});
DisabledLink.args = {
  title: "Disabled link",
  href: "http://some-other-link.nl/super",
  isDisabled: "true",
};
