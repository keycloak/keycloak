import React from 'react';
import {
  Badge,
  MenuToggle,
  Menu,
  MenuContent,
  MenuList,
  MenuItem,
  MenuGroup,
  Popper,
  Divider,
  ToggleGroup,
  ToggleGroupItem,
  Avatar,
  MenuInput
} from '@patternfly/react-core';
import EllipsisVIcon from '@patternfly/react-icons/dist/esm/icons/ellipsis-v-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import avatarImg from 'avatarImg.svg';

export const ComposableDropdwnVariants: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [toggleSelected, setToggleSelected] = React.useState<string>('basic');
  const menuRef = React.useRef<HTMLDivElement>();
  const toggleRef = React.useRef<HTMLButtonElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleToggleSwitch = (selected: boolean, e: React.MouseEvent<any> | React.KeyboardEvent | MouseEvent) => {
    setToggleSelected(e.currentTarget.id);
  };

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (isOpen && menuRef && menuRef.current && menuRef.current.contains(event.target as Node)) {
      if (event.key === 'Escape' || event.key === 'Tab') {
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }
    }
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (isOpen && menuRef && menuRef.current && !menuRef.current.contains(event.target as Node)) {
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
        const firstElement = menuRef.current.querySelector('li > button,input:not(:disabled),a');
        firstElement && (firstElement as HTMLElement).focus();
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  const onSelect = (ev: React.MouseEvent<Element, MouseEvent>, itemId: string) => {
    // eslint-disable-next-line no-console
    console.log(`Menu item ${itemId} selected`);
    setIsOpen(false);
  };

  const buildToggle = () => {
    switch (toggleSelected) {
      case 'basic':
        return (
          <MenuToggle ref={toggleRef} onClick={onToggleClick} isExpanded={isOpen}>
            Dropdown
          </MenuToggle>
        );
      case 'kebab':
        return (
          <MenuToggle
            ref={toggleRef}
            onClick={onToggleClick}
            isExpanded={isOpen}
            variant="plain"
            aria-label="plain kebab"
          >
            <EllipsisVIcon />
          </MenuToggle>
        );
      case 'badge':
        return (
          <MenuToggle
            ref={toggleRef}
            onClick={onToggleClick}
            isExpanded={isOpen}
            variant="plain"
            aria-label="plain badge"
          >
            <Badge>
              4
              <span>
                <CaretDownIcon />
              </span>
            </Badge>
          </MenuToggle>
        );
      case 'image':
        return (
          <MenuToggle
            ref={toggleRef}
            icon={<Avatar src={avatarImg} alt="avatar" />}
            onClick={onToggleClick}
            isExpanded={isOpen}
          >
            Ned Username
          </MenuToggle>
        );
    }
  };

  const menu =
    toggleSelected === 'image' ? (
      <Menu ref={menuRef} id="dropdown-menu" onSelect={onSelect}>
        <MenuInput>Unselectable text displayed at the top of the menu</MenuInput>
        <Divider />
        <MenuContent>
          <MenuItem
            itemId={0}
            to="#default-link0"
            // just for demo so that navigation is not triggered
            onClick={event => event.preventDefault()}
          >
            My profile
          </MenuItem>
          <MenuItem
            itemId={1}
            to="#default-link1"
            // just for demo so that navigation is not triggered
            onClick={event => event.preventDefault()}
          >
            User management
          </MenuItem>
          <MenuItem
            itemId={2}
            to="#default-link2"
            // just for demo so that navigation is not triggered
            onClick={event => event.preventDefault()}
          >
            Logout
          </MenuItem>
        </MenuContent>
      </Menu>
    ) : (
      <Menu ref={menuRef} id="dropdown-menu" onSelect={onSelect}>
        <MenuContent>
          <MenuList>
            <MenuItem
              itemId={0}
              to="#default-link0"
              // just for demo so that navigation is not triggered
              onClick={event => event.preventDefault()}
            >
              Link
            </MenuItem>
            <MenuItem itemId={1}>Action</MenuItem>
            <MenuItem
              itemId={2}
              isDisabled
              to="#default-link2"
              // just for demo so that navigation is not triggered
              onClick={event => event.preventDefault()}
            >
              Disabled link
            </MenuItem>
            <Divider key="group1-divider" />
            <MenuGroup label="Group 1">
              <MenuList>
                <MenuItem itemId={3}>Group 1 action</MenuItem>
                <MenuItem itemId={4} isDisabled>
                  Group 1 disabled action
                </MenuItem>
              </MenuList>
            </MenuGroup>
            <Divider key="group2-divider" />
            <MenuGroup label="Group 2">
              <MenuList>
                <MenuItem itemId={5}>Option 1</MenuItem>
                <MenuItem itemId={6}>Option 2</MenuItem>
              </MenuList>
            </MenuGroup>
          </MenuList>
        </MenuContent>
      </Menu>
    );
  return (
    <React.Fragment>
      <ToggleGroup aria-label="Default with single selectable">
        <ToggleGroupItem
          text="Basic toggle"
          buttonId="basic"
          isSelected={toggleSelected === 'basic'}
          onChange={handleToggleSwitch}
        />
        <ToggleGroupItem
          text="Kebab toggle"
          buttonId="kebab"
          isSelected={toggleSelected === 'kebab'}
          onChange={handleToggleSwitch}
        />
        <ToggleGroupItem
          text="Badge toggle"
          buttonId="badge"
          isSelected={toggleSelected === 'badge'}
          onChange={handleToggleSwitch}
        />
        <ToggleGroupItem
          text="Toggle with image"
          buttonId="image"
          isSelected={toggleSelected === 'image'}
          onChange={handleToggleSwitch}
        />
      </ToggleGroup>
      <br />
      <div ref={containerRef}>
        <Popper
          trigger={buildToggle()}
          popper={menu}
          appendTo={containerRef.current}
          isVisible={isOpen}
          popperMatchesTriggerWidth={['image', 'checkbox'].includes(toggleSelected)}
        />
      </div>
    </React.Fragment>
  );
};
