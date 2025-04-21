import React from 'react';
import { ActionList, ActionListGroup, ActionListItem, Button } from '@patternfly/react-core';

export const ActionListMultipleGroups: React.FunctionComponent = () => (
  <ActionList>
    <ActionListGroup>
      <ActionListItem>
        <Button variant="primary" id="next-button">
          Next
        </Button>
      </ActionListItem>
      <ActionListItem>
        <Button variant="secondary" id="back-button">
          Back
        </Button>
      </ActionListItem>
    </ActionListGroup>
    <ActionListGroup>
      <ActionListItem>
        <Button variant="primary" id="submit-button">
          Submit
        </Button>
      </ActionListItem>
      <ActionListItem>
        <Button variant="link" id="cancel-button">
          Cancel
        </Button>
      </ActionListItem>
    </ActionListGroup>
  </ActionList>
);
