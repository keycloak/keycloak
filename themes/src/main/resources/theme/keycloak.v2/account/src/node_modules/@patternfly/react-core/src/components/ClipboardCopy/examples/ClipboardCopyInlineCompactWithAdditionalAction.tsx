import React from 'react';
import { ClipboardCopy, ClipboardCopyAction, Button } from '@patternfly/react-core';
import PlayIcon from '@patternfly/react-icons/dist/esm/icons/play-icon';

export const ClipboardCopyInlineCompactWithAdditionalAction: React.FunctionComponent = () => (
  <ClipboardCopy
    hoverTip="Copy"
    clickTip="Copied"
    variant="inline-compact"
    additionalActions={
      <ClipboardCopyAction>
        <Button variant="plain" aria-label="Run in web terminal">
          <PlayIcon aria-hidden />
        </Button>
      </ClipboardCopyAction>
    }
  >
    2.3.4-2-redhat
  </ClipboardCopy>
);
