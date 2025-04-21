import React from 'react';
import { Dropdown, DropdownToggle, DropdownItem, DropdownSeparator } from '@patternfly/react-core';

export const DropdownAlignmentOnBreakpoints: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-breakpoints');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
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
    <Dropdown
      onSelect={onSelect}
      alignments={{
        sm: 'left',
        md: 'right',
        lg: 'left',
        xl: 'right',
        '2xl': 'left'
      }}
      toggle={
        <DropdownToggle id="toggle-breakpoints" onToggle={onToggle}>
          Dropdown
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
