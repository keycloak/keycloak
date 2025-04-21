import React from 'react';
import {
  MenuToggle,
  Menu,
  MenuContent,
  MenuList,
  MenuItem,
  MenuGroup,
  MenuInput,
  Popper,
  Tooltip,
  Divider,
  TextInput
} from '@patternfly/react-core';
import { Link } from '@reach/router';
import ThIcon from '@patternfly/react-icons/dist/js/icons/th-icon';
import pfIcon from 'pf-logo-small.svg';

export const ComposableApplicationLauncher: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  const [refFullOptions, setRefFullOptions] = React.useState<Element[]>();
  const [favorites, setFavorites] = React.useState<string[]>([]);
  const [filteredIds, setFilteredIds] = React.useState<string[]>(['*']);
  const menuRef = React.useRef<HTMLDivElement>();
  const toggleRef = React.useRef<HTMLButtonElement>();
  const containerRef = React.useRef<HTMLDivElement>();

  const handleMenuKeys = (event: KeyboardEvent) => {
    if (!isOpen) {
      return;
    }
    if (menuRef.current.contains(event.target as Node) || toggleRef.current.contains(event.target as Node)) {
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

  const onToggleClick = (ev: React.MouseEvent) => {
    ev.stopPropagation(); // Stop handleClickOutside from handling
    setTimeout(() => {
      if (menuRef.current) {
        const firstElement = menuRef.current.querySelector('li > button,input:not(:disabled)');
        firstElement && (firstElement as HTMLElement).focus();
        setRefFullOptions(Array.from(menuRef.current.querySelectorAll('li:not(li[role=separator])')));
      }
    }, 0);
    setIsOpen(!isOpen);
  };

  React.useEffect(() => {
    window.addEventListener('keydown', handleMenuKeys);
    window.addEventListener('click', handleClickOutside);

    return () => {
      window.removeEventListener('keydown', handleMenuKeys);
      window.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen, menuRef]);

  const toggle = (
    <MenuToggle
      aria-label="Toggle"
      ref={toggleRef}
      variant="plain"
      onClick={onToggleClick}
      isExpanded={isOpen}
      style={{ width: 'auto' }}
    >
      <ThIcon />
    </MenuToggle>
  );

  const menuItems = [
    <MenuGroup key="group1" label="Group 1">
      <MenuList>
        <MenuItem itemId="0" id="0" isFavorited={favorites.includes('0')}>
          Application 1
        </MenuItem>
        <MenuItem
          itemId="1"
          id="1"
          isFavorited={favorites.includes('1')}
          to="#default-link2"
          onClick={ev => ev.preventDefault()}
        >
          Application 2
        </MenuItem>
      </MenuList>
    </MenuGroup>,
    <Divider key="group1-divider" />,
    <MenuGroup key="group2" label="Group 2">
      <MenuList>
        <MenuItem
          itemId="2"
          id="2"
          isFavorited={favorites.includes('2')}
          component={props => <Link {...props} to="#router-link" />}
        >
          @reach/router Link
        </MenuItem>
        <MenuItem
          itemId="3"
          id="3"
          isFavorited={favorites.includes('3')}
          isExternalLink
          icon={<img src={pfIcon} />}
          component={props => <Link {...props} to="#router-link2" />}
        >
          @reach/router Link with icon
        </MenuItem>
      </MenuList>
    </MenuGroup>,
    <Divider key="group2-divider" />,
    <MenuList key="other-items">
      <MenuItem key="tooltip-app" isFavorited={favorites.includes('4')} itemId="4" id="4">
        <Tooltip content={<div>Launch Application 3</div>} position="right">
          <span>Application 3 with tooltip</span>
        </Tooltip>
      </MenuItem>
      <MenuItem key="disabled-app" itemId="5" id="5" isDisabled>
        Unavailable Application
      </MenuItem>
    </MenuList>
  ];

  const createFavorites = (favIds: string[]) => {
    const favorites = [];

    menuItems.forEach(item => {
      if (item.type === MenuList) {
        item.props.children.filter(child => {
          if (favIds.includes(child.props.itemId)) {
            favorites.push(child);
          }
        });
      } else if (item.type === MenuGroup) {
        item.props.children.props.children.filter(child => {
          if (favIds.includes(child.props.itemId)) {
            favorites.push(child);
          }
        });
      } else {
        if (favIds.includes(item.props.itemId)) {
          favorites.push(item);
        }
      }
    });

    return favorites;
  };

  const filterItems = (items: any[], filteredIds: string[]) => {
    if (filteredIds.length === 1 && filteredIds[0] === '*') {
      return items;
    }
    let keepDivider = false;
    const filteredCopy = items
      .map(group => {
        if (group.type === MenuGroup) {
          const filteredGroup = React.cloneElement(group, {
            children: React.cloneElement(group.props.children, {
              children: group.props.children.props.children.filter(child => {
                if (filteredIds.includes(child.props.itemId)) {
                  return child;
                }
              })
            })
          });
          const filteredList = filteredGroup.props.children;
          if (filteredList.props.children.length > 0) {
            keepDivider = true;
            return filteredGroup;
          } else {
            keepDivider = false;
          }
        } else if (group.type === MenuList) {
          const filteredGroup = React.cloneElement(group, {
            children: group.props.children.filter(child => {
              if (filteredIds.includes(child.props.itemId)) {
                return child;
              }
            })
          });
          if (filteredGroup.props.children.length > 0) {
            keepDivider = true;
            return filteredGroup;
          } else {
            keepDivider = false;
          }
        } else {
          if ((keepDivider && group.type === Divider) || filteredIds.includes(group.props.itemId)) {
            return group;
          }
        }
      })
      .filter(newGroup => newGroup);

    if (filteredCopy.length > 0) {
      const lastGroup = filteredCopy.pop();
      if (lastGroup.type !== Divider) {
        filteredCopy.push(lastGroup);
      }
    }

    return filteredCopy;
  };

  const onTextChange = (textValue: string) => {
    if (textValue === '') {
      setFilteredIds(['*']);
      return;
    }

    const filteredIds = refFullOptions
      .filter(item => (item as HTMLElement).innerText.toLowerCase().includes(textValue.toString().toLowerCase()))
      .map(item => item.id);
    setFilteredIds(filteredIds);
  };

  const onFavorite = (event: any, itemId: string, actionId: string) => {
    event.stopPropagation();
    if (actionId === 'fav') {
      const isFavorite = favorites.includes(itemId);
      if (isFavorite) {
        setFavorites(favorites.filter(fav => fav !== itemId));
      } else {
        setFavorites([...favorites, itemId]);
      }
    }
  };

  const filteredFavorites = filterItems(createFavorites(favorites), filteredIds);
  const filteredItems = filterItems(menuItems, filteredIds);
  if (filteredItems.length === 0) {
    filteredItems.push(<MenuItem key="no-items">No results found</MenuItem>);
  }

  const menu = (
    // eslint-disable-next-line no-console
    <Menu ref={menuRef} onActionClick={onFavorite} onSelect={(_ev, itemId) => console.log('selected', itemId)}>
      <MenuInput>
        <TextInput
          aria-label="Filter menu items"
          iconVariant="search"
          type="search"
          onChange={value => onTextChange(value)}
        />
      </MenuInput>
      <Divider />
      <MenuContent>
        {filteredFavorites.length > 0 && (
          <React.Fragment>
            <MenuGroup key="favorites-group" label="Favorites">
              <MenuList>{filteredFavorites}</MenuList>
            </MenuGroup>
            <Divider key="favorites-divider" />
          </React.Fragment>
        )}
        {filteredItems}
      </MenuContent>
    </Menu>
  );
  return (
    <div ref={containerRef}>
      <Popper
        trigger={toggle}
        popper={menu}
        isVisible={isOpen}
        popperMatchesTriggerWidth={false}
        appendTo={containerRef.current}
      />
    </div>
  );
};
