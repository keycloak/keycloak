import React from 'react';
import { Dropdown, DropdownToggle, DropdownItem, DropdownSeparator, Flex } from '@patternfly/react-core';

export const DropdownAppendBodyVsParent: React.FunctionComponent = () => {
  const [isBodyOpen, setIsBodyOpen] = React.useState(false);
  const [isParentOpen, setIsParentOpen] = React.useState(false);

  const onBodyToggle = (isBodyOpen: boolean) => {
    setIsBodyOpen(isBodyOpen);
  };
  const onParentToggle = (isParentOpen: boolean) => {
    setIsParentOpen(isParentOpen);
  };

  const onBodyFocus = () => {
    const element = document.getElementById('toggle-append-body');
    element.focus();
  };
  const onParentFocus = () => {
    const element = document.getElementById('toggle-append-parent');
    element.focus();
  };

  const onBodySelect = () => {
    setIsBodyOpen(false);
    onBodyFocus();
  };
  const onParentSelect = () => {
    setIsParentOpen(false);
    onParentFocus();
  };

  const dropdownItems = [
    <DropdownItem key="link">Link</DropdownItem>,
    <DropdownItem key="action" component="button">
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled href="www.google.com">
      Disabled link
    </DropdownItem>,
    <DropdownItem
      key="disabled action"
      isAriaDisabled
      component="button"
      tooltip="Tooltip for disabled item"
      tooltipProps={{ position: 'top' }}
    >
      Disabled action
    </DropdownItem>,
    <DropdownSeparator key="separator" />,
    <DropdownItem key="separated link">Separated link</DropdownItem>,
    <DropdownItem key="separated action" component="button">
      Separated action
    </DropdownItem>
  ];

  return (
    <Flex>
      <Dropdown
        onSelect={onBodySelect}
        toggle={
          <DropdownToggle id="toggle-append-body" onToggle={onBodyToggle}>
            Dropdown appended to body
          </DropdownToggle>
        }
        isOpen={isBodyOpen}
        dropdownItems={dropdownItems}
        menuAppendTo={() => document.body}
      />
      <Dropdown
        onSelect={onParentSelect}
        toggle={
          <DropdownToggle id="toggle-append-parent" onToggle={onParentToggle}>
            Dropdown appended to parent
          </DropdownToggle>
        }
        isOpen={isParentOpen}
        dropdownItems={dropdownItems}
        menuAppendTo="parent"
      />
    </Flex>
  );
};
