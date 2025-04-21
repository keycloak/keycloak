import React from 'react';
import { ActionList, ActionListItem, Button } from '@patternfly/react-core';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';

export const ActionListWithIcons: React.FunctionComponent = () => (
  <ActionList isIconList>
    <ActionListItem>
      <Button variant="plain" id="times-button" aria-label="times icon button">
        <TimesIcon />
      </Button>
    </ActionListItem>
    <ActionListItem>
      <Button variant="plain" id="check-button" aria-label="check icon button">
        <CheckIcon />
      </Button>
    </ActionListItem>
  </ActionList>
);
