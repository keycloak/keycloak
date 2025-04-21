import * as React from 'react';
import { ApplicationLauncherSeparator } from '../components/ApplicationLauncher/ApplicationLauncherSeparator';
import { Divider } from '../components/Divider/Divider';

/**
 * This function is a helper for creating an array of renderable favorite items for the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 * @param {boolean} isEnterTriggersArrowDown Flag indicating if we should add isEnterTriggersArrowDown to favorited item
 */
export const createRenderableFavorites = (
  items: object,
  isGrouped: boolean,
  favorites: any[],
  isEnterTriggersArrowDown?: boolean
) => {
  if (isGrouped) {
    const favoriteItems: React.ReactNode[] = [];
    (items as React.ReactElement[]).forEach(group => {
      if (favorites.length > 0) {
        return (
          group.props.children &&
          (group.props.children as React.ReactElement[])
            .filter(item => favorites.includes(item.props.id))
            .map(item => {
              if (isEnterTriggersArrowDown) {
                return favoriteItems.push(
                  React.cloneElement(item, {
                    isFavorite: true,
                    enterTriggersArrowDown: isEnterTriggersArrowDown,
                    id: `favorite-${item.props.id}`
                  })
                );
              } else {
                return favoriteItems.push(
                  React.cloneElement(item, { isFavorite: true, id: `favorite-${item.props.id}` })
                );
              }
            })
        );
      }
    });
    return favoriteItems;
  }
  return (items as React.ReactElement[])
    .filter(item => favorites.includes(item.props.id))
    .map(item => React.cloneElement(item, { isFavorite: true, enterTriggersArrowDown: isEnterTriggersArrowDown }));
};

/**
 * This function is a helper for extending the array of renderable favorite with the select/application launcher items to  render in the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 */
export const extendItemsWithFavorite = (items: object, isGrouped: boolean, favorites: any[]) => {
  if (isGrouped) {
    return (items as React.ReactElement[]).map(group =>
      React.cloneElement(group, {
        children: React.Children.map(group.props.children as React.ReactElement[], item => {
          if (item.type === ApplicationLauncherSeparator || item.type === Divider) {
            return item;
          }
          return React.cloneElement(item, {
            isFavorite: favorites.some(
              favoriteId => favoriteId === item.props.id || `favorite-${favoriteId}` === item.props.id
            )
          });
        })
      })
    );
  }
  return (items as React.ReactElement[]).map(item =>
    React.cloneElement(item, {
      isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
    })
  );
};
