import React from 'react';
import {
  Hint,
  HintTitle,
  HintBody,
  HintFooter,
  Button,
  Dropdown,
  DropdownItem,
  DropdownSeparator,
  KebabToggle
} from '@patternfly/react-core';

export const HintBasicWithTitle: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onSelect = () => {
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
  const actions = (
    <Dropdown
      onSelect={onSelect}
      toggle={<KebabToggle onToggle={onToggle} id="hint-kebab-toggle" />}
      isOpen={isOpen}
      dropdownItems={dropdownItems}
      position="right"
      isPlain
    />
  );
  return (
    <Hint actions={actions}>
      <HintTitle>Do more with Find it Fix it capabilities</HintTitle>
      <HintBody>
        Upgrade to Red Hat Smart Management to remediate all your systems across regions and geographies.
      </HintBody>
      <HintFooter>
        <Button variant="link" isInline>
          Try it for 90 days
        </Button>
      </HintFooter>
    </Hint>
  );
};
