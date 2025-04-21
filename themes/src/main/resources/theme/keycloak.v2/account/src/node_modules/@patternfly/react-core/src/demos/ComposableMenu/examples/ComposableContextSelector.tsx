import React from 'react';
import {
  MenuToggle,
  Menu,
  MenuContent,
  MenuFooter,
  MenuList,
  MenuItem,
  MenuInput,
  Popper,
  Divider,
  TextInput,
  InputGroup,
  Button,
  ButtonVariant
} from '@patternfly/react-core';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';

interface ItemData {
  text: string;
  href?: string;
  isDisabled?: boolean;
}

type ItemArrayType = (ItemData | string)[];

export const ComposableContextSelector: React.FunctionComponent = () => {
  const items: ItemArrayType = [
    {
      text: 'Action'
    },
    {
      text: 'Link',
      href: '#'
    },
    {
      text: 'Disabled action',
      isDisabled: true
    },
    {
      text: 'Disabled link',
      href: '#',
      isDisabled: true
    },
    'My project',
    'OpenShift cluster',
    'Production Ansible',
    'AWS',
    'Azure',
    'My project 2',
    'OpenShift cluster ',
    'Production Ansible 2 ',
    'AWS 2',
    'Azure 2'
  ];
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [selected, setSelected] = React.useState<ItemData | string>(
    typeof items[0] === 'string' ? items[0] : items[0].text
  );
  const [filteredItems, setFilteredItems] = React.useState<ItemArrayType>(items);
  const [searchInputValue, setSearchInputValue] = React.useState<string>('');
  const menuRef = React.useRef<HTMLDivElement>();
  const toggleRef = React.useRef<HTMLButtonElement>();
  const menuFooterBtnRef = React.useRef<HTMLButtonElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (!isOpen) {
      return;
    }
    if (menuFooterBtnRef.current.contains(event.target as Node)) {
      if (event.key === 'Tab') {
        if (event.shiftKey) {
          return;
        }
        setIsOpen(!isOpen);
        toggleRef.current.focus();
      }
    }
    if (menuRef.current.contains(event.target as Node)) {
      if (event.key === 'Escape') {
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
      {selected}
    </MenuToggle>
  );

  const onSelect = (ev: React.MouseEvent<Element, MouseEvent>, itemId: string) => {
    setSelected(itemId);
    setIsOpen(!isOpen);
  };

  const onSearchInputChange = (value: string) => {
    setSearchInputValue(value);
  };

  const onSearchButtonClick = () => {
    const filtered =
      searchInputValue === ''
        ? items
        : items.filter(item => {
            const str = typeof item === 'string' ? item : item.text;
            return str.toLowerCase().indexOf(searchInputValue.toLowerCase()) !== -1;
          });

    setFilteredItems(filtered || []);
    setIsOpen(true); // Keep menu open after search executed
  };

  const onEnterPressed = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      onSearchButtonClick();
    }
  };

  const menu = (
    <Menu
      ref={menuRef}
      id="context-selector"
      onSelect={onSelect}
      isScrollable
      style={
        {
          '--pf-c-menu--Width': '300px'
        } as React.CSSProperties
      }
    >
      <MenuInput>
        <InputGroup>
          <TextInput
            value={searchInputValue}
            type="search"
            placeholder="Search"
            onChange={onSearchInputChange}
            onKeyPress={onEnterPressed}
            aria-labelledby="pf-context-selector-search-button-id-1"
          />
          <Button
            variant={ButtonVariant.control}
            aria-label="Search menu items"
            id="pf-context-selector-search-button-id-1"
            onClick={onSearchButtonClick}
          >
            <SearchIcon aria-hidden="true" />
          </Button>
        </InputGroup>
      </MenuInput>
      <Divider />
      <MenuContent maxMenuHeight="200px">
        <MenuList>
          {filteredItems.map((item, index) => {
            const [itemText, isDisabled, href] =
              typeof item === 'string' ? [item, null, null] : [item.text, item.isDisabled || null, item.href || null];
            return (
              <MenuItem itemId={itemText} key={index} isDisabled={isDisabled} to={href}>
                {itemText}
              </MenuItem>
            );
          })}
        </MenuList>
      </MenuContent>
      <MenuFooter>
        <Button ref={menuFooterBtnRef} variant="link" isInline>
          Action
        </Button>
      </MenuFooter>
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
