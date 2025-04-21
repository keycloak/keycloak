import React from 'react';
import {
  ActionList,
  ActionListItem,
  Button,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';

export const ActionListSingleGroup: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (
    isOpen: boolean,
    event: MouseEvent | TouchEvent | KeyboardEvent | React.KeyboardEvent<any> | React.MouseEvent<HTMLButtonElement>
  ) => {
    event.stopPropagation();
    setIsOpen(isOpen);
  };

  const onSelect = (event: React.SyntheticEvent<HTMLDivElement, Event>) => {
    event.stopPropagation();
    setIsOpen(!isOpen);
  };

  const dropdownItems = [
    <DropdownItem key="link">Link</DropdownItem>,
    <DropdownItem key="action" component="button">
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled>
      Disabled Link
    </DropdownItem>,
    <DropdownItem key="disabled action" isDisabled component="button">
      Disabled Action
    </DropdownItem>,
    <DropdownSeparator key="separator" />,
    <DropdownItem key="separated link">Separated Link</DropdownItem>,
    <DropdownItem key="separated action" component="button">
      Separated Action
    </DropdownItem>
  ];

  return (
    <React.Fragment>
      <ActionList>
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
      </ActionList>
      <br />
      With kebab
      <ActionList>
        <ActionListItem>
          <Button variant="primary" id="next-button2">
            Next
          </Button>
        </ActionListItem>
        <ActionListItem>
          <Button variant="secondary" id="back-button2">
            Back
          </Button>
        </ActionListItem>
        <ActionListItem>
          <Dropdown
            onSelect={onSelect}
            toggle={<KebabToggle onToggle={onToggle} />}
            isOpen={isOpen}
            isPlain
            dropdownItems={dropdownItems}
            position="right"
          />
        </ActionListItem>
      </ActionList>
    </React.Fragment>
  );
};
