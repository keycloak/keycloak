import React, { useState } from "react";
import type { Meta } from "@storybook/react";

import { DownloadDialog } from "../components/download-dialog/DownloadDialog";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "Download Dialog",
  component: DownloadDialog,
} as Meta;

const Test = () => {
  const [open, setOpen] = useState(false);
  const toggle = () => setOpen(!open);

  return (
    <MockAdminClient
      mock={{ clients: { getInstallationProviders: () => '{some: "json"}' } }}
    >
      <button id="show" onClick={toggle}>
        Show
      </button>
      <DownloadDialog
        id="58577281-7af7-410c-a085-61ff3040be6d"
        open={open}
        toggleDialog={toggle}
      />
    </MockAdminClient>
  );
};

export const Show = () => <Test />;
