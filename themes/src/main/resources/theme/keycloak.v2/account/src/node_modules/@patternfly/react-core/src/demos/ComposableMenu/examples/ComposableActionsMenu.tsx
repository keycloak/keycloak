import React from 'react';
import { MenuToggle, Menu, MenuList, MenuItem, MenuGroup, MenuItemAction, Popper } from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import ClipboardIcon from '@patternfly/react-icons/dist/esm/icons/clipboard-icon';
import CodeBranchIcon from '@patternfly/react-icons/dist/esm/icons/code-branch-icon';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';

export const ComposableActionsMenu: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [selectedItems, setSelectedItems] = React.useState<number[]>([]);
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

  const onSelect = (ev: React.MouseEvent<Element, MouseEvent>, itemId: number) => {
    if (selectedItems.includes(itemId)) {
      setSelectedItems(selectedItems.filter(id => id !== itemId));
    } else {
      setSelectedItems([...selectedItems, itemId]);
    }
  };

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
    <Menu
      ref={menuRef}
      // eslint-disable-next-line no-console
      onActionClick={(event, itemId, actionId) => console.log(`clicked on ${itemId} - ${actionId}`)}
      onSelect={onSelect}
      style={
        {
          '--pf-c-menu--Width': '300px'
        } as React.CSSProperties
      }
    >
      <MenuGroup label="Actions">
        <MenuList>
          <MenuItem
            isSelected={selectedItems.includes(0)}
            actions={
              <MenuItemAction
                icon={<CodeBranchIcon aria-hidden />}
                actionId="code"
                // eslint-disable-next-line no-console
                onClick={() => console.log('clicked on code icon')}
                aria-label="Code"
              />
            }
            description="This is a description"
            itemId={0}
          >
            Item 1
          </MenuItem>
          <MenuItem
            isDisabled
            isSelected={selectedItems.includes(1)}
            actions={<MenuItemAction icon={<BellIcon aria-hidden />} actionId="alert" aria-label="Alert" />}
            description="This is a description"
            itemId={1}
          >
            Item 2
          </MenuItem>
          <MenuItem
            isSelected={selectedItems.includes(2)}
            actions={<MenuItemAction icon={<ClipboardIcon aria-hidden />} actionId="copy" aria-label="Copy" />}
            itemId={2}
          >
            Item 3
          </MenuItem>
          <MenuItem
            isSelected={selectedItems.includes(3)}
            actions={<MenuItemAction icon={<BarsIcon aria-hidden />} actionId="expand" aria-label="Expand" />}
            description="This is a description"
            itemId={3}
          >
            Item 4
          </MenuItem>
        </MenuList>
      </MenuGroup>
    </Menu>
  );

  return (
    <div ref={containerRef}>
      <Popper
        trigger={toggle}
        popper={menu}
        isVisible={isOpen}
        appendTo={containerRef.current}
        popperMatchesTriggerWidth={false}
      />
    </div>
  );
};
