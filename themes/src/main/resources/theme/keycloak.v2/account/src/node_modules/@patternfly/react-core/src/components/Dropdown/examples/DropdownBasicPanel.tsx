import React from 'react';
import { Dropdown, DropdownToggle } from '@patternfly/react-core';

export const DropdownBasicPanel: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById('toggle-panel');
    element.focus();
  };

  const onSelect = () => {
    setIsOpen(false);
    onFocus();
  };

  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle id="toggle-panel" onToggle={onToggle}>
          Expanded dropdown
        </DropdownToggle>
      }
      isOpen={isOpen}
    >
      [Panel contents here]
    </Dropdown>
  );
};
