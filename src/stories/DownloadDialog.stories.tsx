import React from "react";
import { Meta } from "@storybook/react";

import {
  DownloadDialog,
  useDownloadDialog,
} from "../components/download-dialog/DownloadDialog";

export default {
  title: "Download Dialog",
  component: DownloadDialog,
} as Meta;

const Test = () => {
  const [toggle, Dialog] = useDownloadDialog({
    id: "58577281-7af7-410c-a085-61ff3040be6d",
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

export const Show = () => <Test />;
