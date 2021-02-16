import React from "react";
import { Meta, Story } from "@storybook/react";
import {
  FormattedLink,
  FormattedLinkProps,
} from "../components/external-link/FormattedLink";

export default {
  title: "Formatted link",
  component: FormattedLink,
} as Meta;

const Template: Story<FormattedLinkProps> = (args) => (
  <FormattedLink {...args} />
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
