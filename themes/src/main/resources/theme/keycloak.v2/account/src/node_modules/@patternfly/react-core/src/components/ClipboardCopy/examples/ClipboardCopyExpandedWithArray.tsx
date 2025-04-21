import React from 'react';
import { ClipboardCopy, ClipboardCopyVariant } from '@patternfly/react-core';

const text = [
  'Got a lot of text here,',
  'need to see all of it?',
  'Click that arrow on the left side and check out the resulting expansion.'
];

export const ClipboardCopyExpandedWithArray: React.FunctionComponent = () => (
  <ClipboardCopy hoverTip="Copy" clickTip="Copied" variant={ClipboardCopyVariant.expansion}>
    {text.join(' ')}
  </ClipboardCopy>
);
