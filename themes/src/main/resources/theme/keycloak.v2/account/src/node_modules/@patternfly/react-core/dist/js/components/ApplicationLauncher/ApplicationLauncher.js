"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ApplicationLauncher = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const app_launcher_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));
const form_control_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));
const th_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/th-icon'));
const Dropdown_1 = require("../Dropdown");
const DropdownWithContext_1 = require("../Dropdown/DropdownWithContext");
const ApplicationLauncherGroup_1 = require("./ApplicationLauncherGroup");
const ApplicationLauncherSeparator_1 = require("./ApplicationLauncherSeparator");
const ApplicationLauncherItem_1 = require("./ApplicationLauncherItem");
const ApplicationLauncherContext_1 = require("./ApplicationLauncherContext");
const favorites_1 = require("../../helpers/favorites");
class ApplicationLauncher extends React.Component {
    constructor() {
        super(...arguments);
        this.createSearchBox = () => {
            const { onSearch, searchPlaceholderText, searchProps } = this.props;
            return (React.createElement("div", { key: "search", className: react_styles_1.css(app_launcher_1.default.appLauncherMenuSearch) },
                React.createElement(ApplicationLauncherItem_1.ApplicationLauncherItem, { customChild: React.createElement("input", Object.assign({ type: "search", className: react_styles_1.css(form_control_1.default.formControl), placeholder: searchPlaceholderText, onChange: e => onSearch(e.target.value) }, searchProps)) })));
        };
    }
    render() {
        const _a = this.props, { 'aria-label': ariaLabel, isOpen, onToggle, toggleIcon, toggleId, onSelect, isDisabled, className, isGrouped, favorites, onFavorite, onSearch, items, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        searchPlaceholderText, searchProps, ref, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        favoritesLabel, searchNoResultsText, menuAppendTo } = _a, props = tslib_1.__rest(_a, ['aria-label', "isOpen", "onToggle", "toggleIcon", "toggleId", "onSelect", "isDisabled", "className", "isGrouped", "favorites", "onFavorite", "onSearch", "items", "searchPlaceholderText", "searchProps", "ref", "favoritesLabel", "searchNoResultsText", "menuAppendTo"]);
        let renderableItems = [];
        if (onFavorite) {
            let favoritesGroup = [];
            let renderableFavorites = [];
            if (favorites.length > 0) {
                renderableFavorites = favorites_1.createRenderableFavorites(items, isGrouped, favorites, true);
                favoritesGroup = [
                    React.createElement(ApplicationLauncherGroup_1.ApplicationLauncherGroup, { key: "favorites", label: favoritesLabel },
                        renderableFavorites,
                        React.createElement(ApplicationLauncherSeparator_1.ApplicationLauncherSeparator, { key: "separator" }))
                ];
            }
            if (renderableFavorites.length > 0) {
                renderableItems = favoritesGroup.concat(favorites_1.extendItemsWithFavorite(items, isGrouped, favorites));
            }
            else {
                renderableItems = favorites_1.extendItemsWithFavorite(items, isGrouped, favorites);
            }
        }
        else {
            renderableItems = items;
        }
        if (items.length === 0) {
            renderableItems = [
                React.createElement(ApplicationLauncherGroup_1.ApplicationLauncherGroup, { key: "no-results-group" },
                    React.createElement(ApplicationLauncherItem_1.ApplicationLauncherItem, { key: "no-results" }, searchNoResultsText))
            ];
        }
        if (onSearch) {
            renderableItems = [this.createSearchBox(), ...renderableItems];
        }
        return (React.createElement(ApplicationLauncherContext_1.ApplicationLauncherContext.Provider, { value: { onFavorite } },
            React.createElement(Dropdown_1.DropdownContext.Provider, { value: {
                    onSelect,
                    menuClass: app_launcher_1.default.appLauncherMenu,
                    itemClass: app_launcher_1.default.appLauncherMenuItem,
                    toggleClass: app_launcher_1.default.appLauncherToggle,
                    baseClass: app_launcher_1.default.appLauncher,
                    baseComponent: 'nav',
                    sectionClass: app_launcher_1.default.appLauncherGroup,
                    sectionTitleClass: app_launcher_1.default.appLauncherGroupTitle,
                    sectionComponent: 'section',
                    disabledClass: app_launcher_1.default.modifiers.disabled,
                    ouiaComponentType: ApplicationLauncher.displayName
                } },
                React.createElement(DropdownWithContext_1.DropdownWithContext, Object.assign({}, props, { dropdownItems: renderableItems, isOpen: isOpen, className: className, "aria-label": ariaLabel, menuAppendTo: menuAppendTo, toggle: React.createElement(Dropdown_1.DropdownToggle, { id: toggleId, toggleIndicator: null, isOpen: isOpen, onToggle: onToggle, isDisabled: isDisabled, "aria-label": ariaLabel }, toggleIcon), isGrouped: isGrouped })))));
    }
}
exports.ApplicationLauncher = ApplicationLauncher;
ApplicationLauncher.displayName = 'ApplicationLauncher';
ApplicationLauncher.defaultProps = {
    className: '',
    isDisabled: false,
    direction: Dropdown_1.DropdownDirection.down,
    favorites: [],
    isOpen: false,
    position: Dropdown_1.DropdownPosition.left,
    /* eslint-disable @typescript-eslint/no-unused-vars */
    onSelect: (_event) => undefined,
    onToggle: (_value) => undefined,
    /* eslint-enable @typescript-eslint/no-unused-vars */
    'aria-label': 'Application launcher',
    isGrouped: false,
    toggleIcon: React.createElement(th_icon_1.default, null),
    searchPlaceholderText: 'Filter by name...',
    searchNoResultsText: 'No results found',
    favoritesLabel: 'Favorites',
    menuAppendTo: 'inline'
};
//# sourceMappingURL=ApplicationLauncher.js.map