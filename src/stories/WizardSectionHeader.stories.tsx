import React from "react";
import type { Meta, Story } from "@storybook/react";
import {
  WizardSectionHeader,
  WizardSectionHeaderProps,
} from "../components/wizard-section-header/WizardSectionHeader";

export default {
  title: "Wizard Section Header",
  component: WizardSectionHeader,
} as Meta;

const Template: Story<WizardSectionHeaderProps> = (args) => (
  <WizardSectionHeader {...args} />
);

export const TitleAndDescription = Template.bind({});
TitleAndDescription.args = {
  title: "Section title",
  description: "This is a description of the section",
  showDescription: true,
};
