import * as React from 'react';
/**
 * This function is a helper for creating an array of renderable favorite items for the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 * @param {boolean} isEnterTriggersArrowDown Flag indicating if we should add isEnterTriggersArrowDown to favorited item
 */
export declare const createRenderableFavorites: (items: object, isGrouped: boolean, favorites: any[], isEnterTriggersArrowDown?: boolean) => React.ReactNode[];
/**
 * This function is a helper for extending the array of renderable favorite with the select/application launcher items to  render in the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 */
export declare const extendItemsWithFavorite: (items: object, isGrouped: boolean, favorites: any[]) => React.ReactElement<any, string | React.JSXElementConstructor<any>>[];
//# sourceMappingURL=favorites.d.ts.map