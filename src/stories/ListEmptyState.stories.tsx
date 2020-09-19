import React from "react";
import { Meta, Story } from "@storybook/react";
import {
  ListEmptyState,
  ListEmptyStateProps,
} from "../components/list-empty-state/ListEmptyState";

function handleClick() {
  alert("Button clicked to add a thing.");
}

export default {
  title: "List empty state",
  component: ListEmptyState,
} as Meta;

const Template: Story<ListEmptyStateProps> = (args) => (
  <ListEmptyState {...args} />
);

export const View = Template.bind({});
View.args = {
  message: "No things",
  instructions: "You haven't created any things for this list.",
  primaryActionText: "Add a thing",
  onPrimaryAction: handleClick,
};
