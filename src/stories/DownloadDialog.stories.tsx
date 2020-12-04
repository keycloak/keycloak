import React from "react";
import { Meta } from "@storybook/react";

import serverInfo from "../context/server-info/__tests__/mock.json";
import { ServerInfoContext } from "../context/server-info/ServerInfoProvider";

import {
  DownloadDialog,
  useDownloadDialog,
} from "../components/download-dialog/DownloadDialog";
import { MockAdminClient } from "./MockAdminClient";

export default {
  title: "Download Dialog",
  component: DownloadDialog,
} as Meta;

const Test = () => {
  const [toggle, Dialog] = useDownloadDialog({
    id: "58577281-7af7-410c-a085-61ff3040be6d",
  });
  return (
    <ServerInfoContext.Provider value={serverInfo}>
      <MockAdminClient
        mock={{ clients: { getInstallationProviders: () => '{some: "json"}' } }}
      >
        <button id="show" onClick={toggle}>
          Show
        </button>
        <Dialog />
      </MockAdminClient>
    </ServerInfoContext.Provider>
  );
};

export const Show = () => <Test />;
