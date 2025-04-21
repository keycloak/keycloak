import React from 'react';
import { MenuToggle, Menu, MenuContent, MenuList, MenuItem, Popper } from '@patternfly/react-core';

export const ComposableSimpleDropdown: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selected, setSelected] = React.useState<number>(0);
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

  const monthStrings = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December'
  ];

  const dateString = (date: Date) => `${monthStrings[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;

  const date = new Date();

  const toggleText = {
    0: 'Today ',
    1: 'Yesterday ',
    2: 'Last 7 days ',
    3: 'Last 14 days '
  };

  const dateText = {
    0: <small className="pf-u-color-200">({dateString(date)})</small>,
    1: (
      <small className="pf-u-color-200">
        ({dateString(new Date(new Date().setDate(date.getDate() - 1)))} - {dateString(date)})
      </small>
    ),
    2: (
      <small className="pf-u-color-200">
        ({dateString(new Date(new Date().setDate(date.getDate() - 7)))} - {dateString(date)})
      </small>
    ),
    3: (
      <small className="pf-u-color-200">
        ({dateString(new Date(new Date().setDate(date.getDate() - 14)))} - {dateString(date)})
      </small>
    )
  };

  const toggle = (
    <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen} style={{ minWidth: '250px' }}>
      <span style={{ verticalAlign: 'middle', marginRight: '8px' }}>{toggleText[selected]}</span>
      {dateText[selected]}
    </MenuToggle>
  );
  const menu = (
    // eslint-disable-next-line no-console
    <Menu ref={menuRef} onSelect={(_ev, itemId) => setSelected(itemId as number)} selected={selected}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId={0}>Today</MenuItem>
          <MenuItem itemId={1}>Yesterday</MenuItem>
          <MenuItem itemId={2}>Last 7 days</MenuItem>
          <MenuItem itemId={3}>Last 14 days</MenuItem>
        </MenuList>
      </MenuContent>
    </Menu>
  );
  return (
    <div ref={containerRef}>
      <Popper
        trigger={toggle}
        popper={menu}
        appendTo={containerRef.current}
        isVisible={isOpen}
        popperMatchesTriggerWidth={false}
      />
    </div>
  );
};
