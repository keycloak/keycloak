import React from 'react';
import { ActionList, ActionListGroup, ActionListItem, Button } from '@patternfly/react-core';

export const ActionListWithCancelButton: React.FunctionComponent = () => (
  <React.Fragment>
    In modals, forms, data lists
    <ActionList>
      <ActionListItem>
        <Button variant="primary" id="save-button">
          Save
        </Button>
      </ActionListItem>
      <ActionListItem>
        <Button variant="link" id="cancel-button">
          Cancel
        </Button>
      </ActionListItem>
    </ActionList>
    <br />
    In wizards
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
          <Button variant="link" id="cancel-button2">
            Cancel
          </Button>
        </ActionListItem>
      </ActionListGroup>
    </ActionList>
  </React.Fragment>
);
