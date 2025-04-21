import React from 'react';
import { ClipboardCopy } from '@patternfly/react-core';

export const ClipboardCopyBasic: React.FunctionComponent = () => (
  <ClipboardCopy hoverTip="Copy" clickTip="Copied">
    This is editable
  </ClipboardCopy>
);
