import React from 'react';
import { MenuToggle, Menu, MenuContent, MenuList, MenuItem, Popper } from '@patternfly/react-core';

// eslint-disable-next-line no-console
const onSelect = (ev: React.MouseEvent<Element, MouseEvent>, itemId: string) => console.log('selected', itemId);

interface FlyoutMenuProps {
  children?: React.ReactElement;
  depth: number;
}

const FlyoutMenu: React.FunctionComponent<FlyoutMenuProps> = ({ depth, children }: FlyoutMenuProps) => (
  <Menu key={depth} containsFlyout id={`menu-${depth}`} onSelect={onSelect}>
    <MenuContent>
      <MenuList>
        <MenuItem flyoutMenu={children} itemId={`next-menu-${depth}`}>
          Next menu
        </MenuItem>
        {Array.from(new Array(15 - depth), (x, i) => i + 1).map(j => (
          <MenuItem key={`${depth}-${j}`} itemId={`${depth}-${j}`}>
            Menu {depth} item {j}
          </MenuItem>
        ))}
        <MenuItem flyoutMenu={children} itemId={`next-menu-2-${depth}`}>
          Next menu
        </MenuItem>
      </MenuList>
    </MenuContent>
  </Menu>
);

export const ComposableFlyout: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const menuRef = React.useRef<HTMLDivElement>();
  const toggleRef = React.useRef<HTMLButtonElement>();
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

  let curFlyout = <FlyoutMenu depth={1} />;
  for (let i = 2; i < 14; i++) {
    curFlyout = <FlyoutMenu depth={i}>{curFlyout}</FlyoutMenu>;
  }

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
    <MenuToggle onClick={onToggleClick} isExpanded={isOpen}>
      {isOpen ? 'Expanded' : 'Collapsed'}
    </MenuToggle>
  );

  const menu = (
    <Menu ref={menuRef} containsFlyout onSelect={onSelect}>
      <MenuContent>
        <MenuList>
          <MenuItem itemId="start">Start rollout</MenuItem>
          <MenuItem itemId="pause">Pause rollouts</MenuItem>
          <MenuItem itemId="storage">Add storage</MenuItem>
          <MenuItem description="Description" flyoutMenu={curFlyout} itemId="next-menu-root">
            Edit
          </MenuItem>
          <MenuItem itemId="delete">Delete deployment config</MenuItem>
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
