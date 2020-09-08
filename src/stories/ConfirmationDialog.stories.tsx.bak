import React from "react";
import { Meta, Story } from "@storybook/react";
import {
  ConfirmationDialog,
  ConfirmationDialogProps,
} from "../components/confirmation-dialog/ConfirmationDialog";

export default {
  title: "Confirmation dailog",
  component: ConfirmationDialog,
  parameters: { actions: { argTypesRegex: "^on.*" } },
} as Meta;

const Template: Story<ConfirmationDialogProps> = (args) => (
  <ConfirmationDialog {...args} />
);

export const Dialog = Template.bind({});
Dialog.args = {
  onConfirm: () => "Confirm",
};
