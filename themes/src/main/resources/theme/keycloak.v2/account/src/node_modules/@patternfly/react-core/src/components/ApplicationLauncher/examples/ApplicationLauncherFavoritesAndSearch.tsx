import React from 'react';
import {
  ApplicationLauncher,
  ApplicationLauncherItem,
  ApplicationLauncherGroup,
  ApplicationLauncherSeparator
} from '@patternfly/react-core';
import pfLogoSm from './pf-logo-small.svg';

const icon: JSX.Element = <img src={pfLogoSm} />;

const appLauncherItems: React.ReactElement[] = [
  <ApplicationLauncherGroup key="group 1c">
    <ApplicationLauncherItem key="group 1a" id="item-1" icon={icon}>
      Item without group title
    </ApplicationLauncherItem>
    <ApplicationLauncherSeparator key="separator" />
  </ApplicationLauncherGroup>,
  <ApplicationLauncherGroup label="Group 2" key="group 2c">
    <ApplicationLauncherItem key="group 2a" id="item-2" isExternal icon={icon} component="button">
      Group 2 button
    </ApplicationLauncherItem>
    <ApplicationLauncherItem key="group 2b" id="item-3" isExternal href="#" icon={icon}>
      Group 2 anchor link
    </ApplicationLauncherItem>
    <ApplicationLauncherSeparator key="separator" />
  </ApplicationLauncherGroup>,
  <ApplicationLauncherGroup label="Group 3" key="group 3c">
    <ApplicationLauncherItem key="group 3a" id="item-4" isExternal icon={icon} component="button">
      Group 3 button
    </ApplicationLauncherItem>
    <ApplicationLauncherItem key="group 3b" id="item-5" isExternal href="#" icon={icon}>
      Group 3 anchor link
    </ApplicationLauncherItem>
  </ApplicationLauncherGroup>
];

export const ApplicationLauncherFavoritesAndSearch: React.FunctionComponent = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [favorites, setFavorites] = React.useState<string[]>([]);
  const [filteredItems, setFilteredItems] = React.useState<React.ReactNode[]>(null);

  const onToggle = (isOpen: boolean) => setIsOpen(isOpen);

  const onFavorite = (itemId: string, isFavorite: boolean) => {
    let updatedFavorites: string[] = [...favorites, itemId];

    if (isFavorite) {
      updatedFavorites = favorites.filter(id => id !== itemId);
    }

    setFavorites(updatedFavorites);
  };

  const onSearch = (textInput: string) => {
    if (textInput === '') {
      setFilteredItems(null);
    } else {
      const filteredGroups = appLauncherItems
        .map((group: React.ReactElement) => {
          const filteredGroup = React.cloneElement(group, {
            children: group.props.children.filter((item: React.ReactElement) => {
              if (item.type === ApplicationLauncherSeparator) {
                return item;
              }

              return item.props.children.toLowerCase().includes(textInput.toLowerCase());
            })
          });

          if (
            filteredGroup.props.children.length > 0 &&
            filteredGroup.props.children[0].type !== ApplicationLauncherSeparator
          ) {
            return filteredGroup;
          }
        })
        .filter(newGroup => newGroup);

      if (filteredGroups.length > 0) {
        let lastGroup = filteredGroups.pop();

        lastGroup = React.cloneElement(lastGroup, {
          children: lastGroup.props.children.filter(item => item.type !== ApplicationLauncherSeparator)
        });

        filteredGroups.push(lastGroup);
      }

      setFilteredItems(filteredGroups);
    }
  };

  return (
    <ApplicationLauncher
      onToggle={onToggle}
      onFavorite={onFavorite}
      onSearch={onSearch}
      isOpen={isOpen}
      items={filteredItems || appLauncherItems}
      favorites={favorites}
      isGrouped
    />
  );
};
