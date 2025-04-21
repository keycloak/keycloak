import React from 'react';
import { ClipboardCopy } from '@patternfly/react-core';
export const ClipboardCopyInlineCompact: React.FunctionComponent = () => (
  <ClipboardCopy hoverTip="Copy" clickTip="Copied" variant="inline-compact">
    2.3.4-2-redhat
  </ClipboardCopy>
);
