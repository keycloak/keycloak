import React from 'react';
import { MenuToggle, Menu, MenuContent, MenuList, MenuItem, Popper } from '@patternfly/react-core';

export const ComposableSimpleDropdown: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const toggleRef = React.useRef<HTMLButtonElement>();
  const menuRef = React.useRef<HTMLDivElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (!isOpen) {
      return;
    }
    if (menuRef.current.contains(event.target as Node) || toggleRef.current.contains(event.target as Node)) {
      if (event.key === 'Escape' || event.key === 'Tab') {
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }
    }
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (isOpen && !menuRef.current.contains(event.target as Node)) {
      setIsOpen(false);
    }
  };

  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, menuRef]);

  const onToggleClick = (ev: React.MouseEvent) => {
    ev.stopPropagation(); // Stop handleClickOutside from handling
    setTimeout(() => {
      if (menuRef.current) {
        const firstElement = menuRef.current.querySelector('li > button:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const toggle = (
    <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen}>
      {isOpen ? 'Expanded' : 'Collapsed'}
    </MenuToggle>
  );
  const menu = (
    // eslint-disable-next-line no-console
    <Menu ref={menuRef} onSelect={(_ev, itemId) => console.log('selected', itemId)}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId={0}>Action</MenuItem>
          <MenuItem itemId={1} to="#default-link2" onClick={ev => ev.preventDefault()}>
            Link
          </MenuItem>
          <MenuItem isDisabled>Disabled Action</MenuItem>
          <MenuItem isDisabled to="#default-link4">
            Disabled Link
          </MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );
  return (
    <div ref={containerRef}>
      <Popper trigger={toggle} popper={menu} appendTo={containerRef.current} isVisible={isOpen} />
    </div>
  );
};
