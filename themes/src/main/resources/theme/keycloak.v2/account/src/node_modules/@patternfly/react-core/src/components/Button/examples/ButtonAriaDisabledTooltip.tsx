import React from 'react';
import { Button, Tooltip } from '@patternfly/react-core';

export const ButtonAriaDisabledTooltip: React.FunctionComponent = () => (
  <Tooltip content="Aria-disabled buttons are like disabled buttons, but focusable. Allows for tooltip support.">
    <Button isAriaDisabled variant="secondary">
      Secondary button to core docs
    </Button>
  </Tooltip>
);
