import React from "react";
import { IFormatterValueType } from "@patternfly/react-table";
import { Meta, Story } from "@storybook/react";

import clients from "../clients/__tests__/mock-clients.json";

import {
  KeycloakDataTable,
  DataListProps,
} from "../components/table-toolbar/KeycloakDataTable";

export default {
  title: "Keycloak Data Table",
  component: KeycloakDataTable,
} as Meta;

const wait = (ms: number, value: any) =>
  new Promise((resolve) => setTimeout(resolve, ms, value));

const Template: Story<DataListProps<any>> = (args) => (
  <KeycloakDataTable {...args} />
);

export const SimpleList = Template.bind({});
SimpleList.args = {
  ariaLabelKey: "clients:clientList",
  searchPlaceholderKey: "common:search",
  columns: [
    { name: "clientId", displayKey: "clients:clientID" },
    { name: "protocol", displayKey: "common:type" },
    {
      name: "description",
      displayKey: "common:description",
      cellFormatters: [
        (data?: IFormatterValueType) => {
          return data ? data : "—";
        },
      ],
    },
    { name: "baseUrl", displayKey: "clients:homeURL" },
  ],
  loader: () => clients,
};

export const LoadingList = Template.bind({});
LoadingList.args = {
  ariaLabelKey: "clients:clientList",
  searchPlaceholderKey: "common:search",
  columns: [{ name: "title" }, { name: "body" }],
  isPaginated: true,
  loader: async () => {
    const res = await fetch("https://jsonplaceholder.typicode.com/posts/");
    const value = await res.json();
    return wait(3000, value) as any;
  },
};

export const EmptyList = Template.bind({});
EmptyList.args = {
  ariaLabelKey: "clients:clientList",
  searchPlaceholderKey: "common:search",
  columns: [{ name: "title" }, { name: "body" }],
  loader: () => Promise.resolve([]),
  emptyState: <h1>Wait what? No content?</h1>,
};
