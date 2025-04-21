"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.extendItemsWithFavorite = exports.createRenderableFavorites = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ApplicationLauncherSeparator_1 = require("../components/ApplicationLauncher/ApplicationLauncherSeparator");
const Divider_1 = require("../components/Divider/Divider");
/**
 * This function is a helper for creating an array of renderable favorite items for the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 * @param {boolean} isEnterTriggersArrowDown Flag indicating if we should add isEnterTriggersArrowDown to favorited item
 */
const createRenderableFavorites = (items, isGrouped, favorites, isEnterTriggersArrowDown) => {
    if (isGrouped) {
        const favoriteItems = [];
        items.forEach(group => {
            if (favorites.length > 0) {
                return (group.props.children &&
                    group.props.children
                        .filter(item => favorites.includes(item.props.id))
                        .map(item => {
                        if (isEnterTriggersArrowDown) {
                            return favoriteItems.push(React.cloneElement(item, {
                                isFavorite: true,
                                enterTriggersArrowDown: isEnterTriggersArrowDown,
                                id: `favorite-${item.props.id}`
                            }));
                        }
                        else {
                            return favoriteItems.push(React.cloneElement(item, { isFavorite: true, id: `favorite-${item.props.id}` }));
                        }
                    }));
            }
        });
        return favoriteItems;
    }
    return items
        .filter(item => favorites.includes(item.props.id))
        .map(item => React.cloneElement(item, { isFavorite: true, enterTriggersArrowDown: isEnterTriggersArrowDown }));
};
exports.createRenderableFavorites = createRenderableFavorites;
/**
 * This function is a helper for extending the array of renderable favorite with the select/application launcher items to  render in the Application launcher or Select
 *
 * @param {object} items The items rendered in Select or Application aLauncher
 * @param {boolean} isGrouped Flag indicating if items are grouped
 * @param {any[]} favorites Array of ids of favorited items
 */
const extendItemsWithFavorite = (items, isGrouped, favorites) => {
    if (isGrouped) {
        return items.map(group => React.cloneElement(group, {
            children: React.Children.map(group.props.children, item => {
                if (item.type === ApplicationLauncherSeparator_1.ApplicationLauncherSeparator || item.type === Divider_1.Divider) {
                    return item;
                }
                return React.cloneElement(item, {
                    isFavorite: favorites.some(favoriteId => favoriteId === item.props.id || `favorite-${favoriteId}` === item.props.id)
                });
            })
        }));
    }
    return items.map(item => React.cloneElement(item, {
        isFavorite: favorites.some(favoriteId => favoriteId === item.props.id)
    }));
};
exports.extendItemsWithFavorite = extendItemsWithFavorite;
//# sourceMappingURL=favorites.js.map