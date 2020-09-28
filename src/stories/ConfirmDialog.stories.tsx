import React from "react";
import {
  TextContent,
  Text,
  TextVariants,
  ButtonVariant,
} from "@patternfly/react-core";
import { Meta, Story } from "@storybook/react";
import { action } from "@storybook/addon-actions";

import {
  ConfirmDialogModal,
  ConfirmDialogModalProps,
  useConfirmDialog,
} from "../components/confirm-dialog/ConfirmDialog";

export default {
  title: "Confirmation Dialog",
  component: ConfirmDialogModal,
} as Meta;

const Template: Story<ConfirmDialogModalProps> = (args) => (
  <ConfirmDialogModal {...args} />
);

export const Simple = Template.bind({});
Simple.args = {
  titleKey: "Delete app02?",
  messageKey: "If you delete this client, all associated data will be removed.",
  continueButtonLabel: "Delete",
  continueButtonVariant: ButtonVariant.danger,
};

export const Children = Template.bind({});
Children.args = {
  titleKey: "Children as content!",
  continueButtonVariant: ButtonVariant.primary,
  children: (
    <>
      <TextContent>
        <Text component={TextVariants.h3}>Hello World</Text>
      </TextContent>
      <p>Example of some other patternfly components.</p>
    </>
  ),
};

const Test = () => {
  const [toggle, Dialog] = useConfirmDialog({
    titleKey: "Delete app02?",
    messageKey:
      "If you delete this client, all associated data will be removed.",
    continueButtonLabel: "Delete",
    onConfirm: action("confirm"),
    onCancel: action("cancel"),
  });
  return (
    <>
      <button id="show" onClick={toggle}>
        Show
      </button>
      <Dialog />
    </>
  );
};

export const Api = () => <Test />;
