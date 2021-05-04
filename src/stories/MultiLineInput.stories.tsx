import React from "react";
import type { Meta, Story } from "@storybook/react";
import { action } from "@storybook/addon-actions";
import { FormProvider, useForm } from "react-hook-form";
import { Button } from "@patternfly/react-core";

import {
  MultiLineInput,
  MultiLineInputProps,
  toValue,
} from "../components/multi-line-input/MultiLineInput";

export default {
  title: "MultiLineInput component",
  component: MultiLineInput,
} as Meta;

const Template: Story<MultiLineInputProps> = (args) => {
  const form = useForm({ mode: "onChange" });
  return (
    <form
      onSubmit={form.handleSubmit((data) => {
        action("submit")(toValue(data.items));
      })}
    >
      <FormProvider {...form}>
        <MultiLineInput {...args} />
      </FormProvider>
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
