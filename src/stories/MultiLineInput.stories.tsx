import React from "react";
import { Meta, Story } from "@storybook/react";
import { action } from "@storybook/addon-actions";
import { useForm } from "react-hook-form";
import { Button } from "@patternfly/react-core";

import {
  MultiLineInput,
  MultiLineInputProps,
} from "../components/multi-line-input/MultiLineInput";

export default {
  title: "MultiLineInput component",
  component: MultiLineInput,
} as Meta;

const Template: Story<MultiLineInputProps> = (args) => {
  const form = useForm({
    defaultValues: {
      items: [{ value: "" }],
    },
  });
  return (
    <form
      onSubmit={form.handleSubmit((data) => {
        action("submit")(data);
      })}
    >
      <MultiLineInput {...args} form={form} />
      <br />
      <br />
      <Button type="submit">Submit</Button>
    </form>
  );
};

export const View = Template.bind({});
View.args = {
  name: "items",
};
