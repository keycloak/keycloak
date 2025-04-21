import React from 'react';
import { MenuToggle, Menu, MenuContent, MenuList, MenuItem, Popper } from '@patternfly/react-core';
import TableIcon from '@patternfly/react-icons/dist/esm/icons/table-icon';

export const ComposableSimpleSelect: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [selected, setSelected] = React.useState<string>('Select a value');
  const toggleRef = React.useRef<HTMLButtonElement>();
  const menuRef = React.useRef<HTMLDivElement>();
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
        const firstElement = menuRef.current.querySelector('li > button:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const toggle = (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={isOpen}
      style={
        {
          width: '200px'
        } as React.CSSProperties
      }
    >
      {selected}
    </MenuToggle>
  );
  const menu = (
    <Menu ref={menuRef} id="select-menu" onSelect={(_ev, itemId) => setSelected(itemId.toString())} selected={selected}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId="Option 1">Option 1</MenuItem>
          <MenuItem itemId="Option 2">Option 2</MenuItem>
          <MenuItem itemId="Option 3" icon={<TableIcon aria-hidden />}>
            Option 3
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
