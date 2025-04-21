import React from 'react';
import { MenuToggle, Menu, MenuContent, MenuList, MenuItem, MenuGroup, Popper, Divider } from '@patternfly/react-core';

export const ComposableOptionsMenuVariants: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [selected, setSelected] = React.useState<string>('');
  const menuRef = React.useRef<HTMLDivElement>();
  const toggleRef = React.useRef<HTMLButtonElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (isOpen && menuRef.current.contains(event.target as Node)) {
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
        const firstElement = menuRef.current.querySelector('li > button,input:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const toggle = (
    <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen}>
      Options menu
    </MenuToggle>
  );

  const menu = (
    <Menu
      ref={menuRef}
      id="options-menu"
      selected={selected}
      onSelect={(_ev, itemId) => setSelected(itemId.toString())}
    >
      <MenuContent>
        <MenuList>
          <MenuItem itemId="0" isSelected={selected === '0'}>
            Option 1
          </MenuItem>
          <MenuItem itemId="1" isSelected={selected === '1'} isDisabled>
            Disabled Option
          </MenuItem>
          <Divider key="group1-divider" />
          <MenuGroup label="Group 1">
            <MenuList>
              <MenuItem itemId="2" isSelected={selected === '2'}>
                Option 1
              </MenuItem>
              <MenuItem itemId="3" isSelected={selected === '3'}>
                Option 2
              </MenuItem>
            </MenuList>
          </MenuGroup>
          <Divider key="group2-divider" />
          <MenuGroup label="Group 2">
            <MenuList>
              <MenuItem itemId="4" isSelected={selected === '4'}>
                Option 1
              </MenuItem>
              <MenuItem itemId="5" isSelected={selected === '5'}>
                Option 2
              </MenuItem>
            </MenuList>
          </MenuGroup>
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
