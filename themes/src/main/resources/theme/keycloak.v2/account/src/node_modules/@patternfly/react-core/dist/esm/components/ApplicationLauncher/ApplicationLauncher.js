import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/AppLauncher/app-launcher';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import ThIcon from '@patternfly/react-icons/dist/esm/icons/th-icon';
import { DropdownDirection, DropdownPosition, DropdownToggle, DropdownContext } from '../Dropdown';
import { DropdownWithContext } from '../Dropdown/DropdownWithContext';
import { ApplicationLauncherGroup } from './ApplicationLauncherGroup';
import { ApplicationLauncherSeparator } from './ApplicationLauncherSeparator';
import { ApplicationLauncherItem } from './ApplicationLauncherItem';
import { ApplicationLauncherContext } from './ApplicationLauncherContext';
import { createRenderableFavorites, extendItemsWithFavorite } from '../../helpers/favorites';
export class ApplicationLauncher extends React.Component {
    constructor() {
        super(...arguments);
        this.createSearchBox = () => {
            const { onSearch, searchPlaceholderText, searchProps } = this.props;
            return (React.createElement("div", { key: "search", className: css(styles.appLauncherMenuSearch) },
                React.createElement(ApplicationLauncherItem, { customChild: React.createElement("input", Object.assign({ type: "search", className: css(formStyles.formControl), placeholder: searchPlaceholderText, onChange: e => onSearch(e.target.value) }, searchProps)) })));
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, isOpen, onToggle, toggleIcon, toggleId, onSelect, isDisabled, className, isGrouped, favorites, onFavorite, onSearch, items, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        searchPlaceholderText, searchProps, ref, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        favoritesLabel, searchNoResultsText, menuAppendTo } = _a, props = __rest(_a, ['aria-label', "isOpen", "onToggle", "toggleIcon", "toggleId", "onSelect", "isDisabled", "className", "isGrouped", "favorites", "onFavorite", "onSearch", "items", "searchPlaceholderText", "searchProps", "ref", "favoritesLabel", "searchNoResultsText", "menuAppendTo"]);
        let renderableItems = [];
        if (onFavorite) {
            let favoritesGroup = [];
            let renderableFavorites = [];
            if (favorites.length > 0) {
                renderableFavorites = createRenderableFavorites(items, isGrouped, favorites, true);
                favoritesGroup = [
                    React.createElement(ApplicationLauncherGroup, { key: "favorites", label: favoritesLabel },
                        renderableFavorites,
                        React.createElement(ApplicationLauncherSeparator, { key: "separator" }))
                ];
            }
            if (renderableFavorites.length > 0) {
                renderableItems = favoritesGroup.concat(extendItemsWithFavorite(items, isGrouped, favorites));
            }
            else {
                renderableItems = extendItemsWithFavorite(items, isGrouped, favorites);
            }
        }
        else {
            renderableItems = items;
        }
        if (items.length === 0) {
            renderableItems = [
                React.createElement(ApplicationLauncherGroup, { key: "no-results-group" },
                    React.createElement(ApplicationLauncherItem, { key: "no-results" }, searchNoResultsText))
            ];
        }
        if (onSearch) {
            renderableItems = [this.createSearchBox(), ...renderableItems];
        }
        return (React.createElement(ApplicationLauncherContext.Provider, { value: { onFavorite } },
            React.createElement(DropdownContext.Provider, { value: {
                    onSelect,
                    menuClass: styles.appLauncherMenu,
                    itemClass: styles.appLauncherMenuItem,
                    toggleClass: styles.appLauncherToggle,
                    baseClass: styles.appLauncher,
                    baseComponent: 'nav',
                    sectionClass: styles.appLauncherGroup,
                    sectionTitleClass: styles.appLauncherGroupTitle,
                    sectionComponent: 'section',
                    disabledClass: styles.modifiers.disabled,
                    ouiaComponentType: ApplicationLauncher.displayName
                } },
                React.createElement(DropdownWithContext, Object.assign({}, props, { dropdownItems: renderableItems, isOpen: isOpen, className: className, "aria-label": ariaLabel, menuAppendTo: menuAppendTo, toggle: React.createElement(DropdownToggle, { id: toggleId, toggleIndicator: null, isOpen: isOpen, onToggle: onToggle, isDisabled: isDisabled, "aria-label": ariaLabel }, toggleIcon), isGrouped: isGrouped })))));
    }
}
ApplicationLauncher.displayName = 'ApplicationLauncher';
ApplicationLauncher.defaultProps = {
    className: '',
    isDisabled: false,
    direction: DropdownDirection.down,
    favorites: [],
    isOpen: false,
    position: DropdownPosition.left,
    /* eslint-disable @typescript-eslint/no-unused-vars */
    onSelect: (_event) => undefined,
    onToggle: (_value) => undefined,
    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': 'Application launcher',
    isGrouped: false,
    toggleIcon: React.createElement(ThIcon, null),
    searchPlaceholderText: 'Filter by name...',
    searchNoResultsText: 'No results found',
    favoritesLabel: 'Favorites',
    menuAppendTo: 'inline'
};
//# sourceMappingURL=ApplicationLauncher.js.map