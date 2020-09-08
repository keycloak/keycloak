import React from "react";
import { Meta, Story } from "@storybook/react";

import {
  JsonFileUpload,
  JsonFileUploadProps,
} from "../components/json-file-upload/JsonFileUpload";

export default {
  title: "Json file upload dailog",
  component: JsonFileUpload,
  parameters: { actions: { argTypesRegex: "^on.*" } },
} as Meta;

const Template: Story<JsonFileUploadProps> = (args) => (
  <JsonFileUpload {...args} />
);

export const Dialog = Template.bind({});
Dialog.args = {
  id: "jsonFile",
};
