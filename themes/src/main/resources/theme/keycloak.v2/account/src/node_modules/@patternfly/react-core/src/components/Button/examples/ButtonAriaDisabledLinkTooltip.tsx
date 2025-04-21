import React from 'react';
import { Button, Tooltip } from '@patternfly/react-core';

export const ButtonAriaDisabledLinkTooltip: React.FunctionComponent = () => (
  <Tooltip content="Aria-disabled link as button with tooltip">
    <Button component="a" isAriaDisabled href="https://pf4.patternfly.org/" target="_blank" variant="tertiary">
      Tertiary link as button to core docs
    </Button>
  </Tooltip>
);
