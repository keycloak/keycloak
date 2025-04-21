"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ApplicationLauncherItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const app_launcher_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/AppLauncher/app-launcher"));
const Dropdown_1 = require("../Dropdown");
const ApplicationLauncherContent_1 = require("./ApplicationLauncherContent");
const ApplicationLauncherContext_1 = require("./ApplicationLauncherContext");
const ApplicationLauncherItemContext_1 = require("./ApplicationLauncherItemContext");
const star_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/star-icon'));
const util_1 = require("../../helpers/util");
const ApplicationLauncherItem = (_a) => {
    var { className = '', id, children, icon = null, isExternal = false, href, tooltip = null, tooltipProps = null, component = 'a', isFavorite = null, ariaIsFavoriteLabel = 'starred', ariaIsNotFavoriteLabel = 'not starred', customChild, enterTriggersArrowDown = false } = _a, props = tslib_1.__rest(_a, ["className", "id", "children", "icon", "isExternal", "href", "tooltip", "tooltipProps", "component", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "customChild", "enterTriggersArrowDown"]);
    return (React.createElement(ApplicationLauncherItemContext_1.ApplicationLauncherItemContext.Provider, { value: { isExternal, icon } },
        React.createElement(ApplicationLauncherContext_1.ApplicationLauncherContext.Consumer, null, ({ onFavorite }) => (React.createElement(Dropdown_1.DropdownItem, Object.assign({ id: id, component: component, href: href || null, className: react_styles_1.css(isExternal && app_launcher_1.default.modifiers.external, isFavorite !== null && app_launcher_1.default.modifiers.link, className), listItemClassName: react_styles_1.css(onFavorite && app_launcher_1.default.appLauncherMenuWrapper, isFavorite && app_launcher_1.default.modifiers.favorite), tooltip: tooltip, tooltipProps: tooltipProps }, (enterTriggersArrowDown === true && { enterTriggersArrowDown }), (customChild && { customChild }), (isFavorite !== null && {
            additionalChild: (React.createElement("button", { className: react_styles_1.css(app_launcher_1.default.appLauncherMenuItem, app_launcher_1.default.modifiers.action), "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel, onClick: () => {
                    onFavorite((id || util_1.getUniqueId('app-launcher-option')).replace('favorite-', ''), isFavorite);
                } },
                React.createElement(star_icon_1.default, null)))
        }), props), children && React.createElement(ApplicationLauncherContent_1.ApplicationLauncherContent, null, children))))));
};
exports.ApplicationLauncherItem = ApplicationLauncherItem;
exports.ApplicationLauncherItem.displayName = 'ApplicationLauncherItem';
//# sourceMappingURL=ApplicationLauncherItem.js.map