import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

export const ClipboardCopyJSONObject: React.FunctionComponent = () => (
  <ClipboardCopy isCode hoverTip="Copy" clickTip="Copied" variant={ClipboardCopyVariant.expansion}>
    {`{ "menu": {
    "id": "file",
    "value": "File",
    "popup": {
      "menuitem": [
        {"value": "New", "onclick": "CreateNewDoc()"},
        {"value": "Open", "onclick": "OpenDoc()"},
        {"value": "Close", "onclick": "CloseDoc()"}
      ]
    }
  }} `}
  </ClipboardCopy>
);
