"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenuItemGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const options_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OptionsMenu/options-menu"));
const react_styles_1 = require("@patternfly/react-styles");
const Divider_1 = require("../Divider");
const OptionsMenuItemGroup = (_a) => {
    var { className = '', 'aria-label': ariaLabel = '', groupTitle = '', children = null, hasSeparator = false } = _a, props = tslib_1.__rest(_a, ["className", 'aria-label', "groupTitle", "children", "hasSeparator"]);
    return (React.createElement("section", Object.assign({}, props, { className: react_styles_1.css(options_menu_1.default.optionsMenuGroup) }),
        groupTitle && React.createElement("h1", { className: react_styles_1.css(options_menu_1.default.optionsMenuGroupTitle) }, groupTitle),
        React.createElement("ul", { className: className, "aria-label": ariaLabel },
            children,
            hasSeparator && React.createElement(Divider_1.Divider, { component: "li", role: "separator" }))));
};
exports.OptionsMenuItemGroup = OptionsMenuItemGroup;
exports.OptionsMenuItemGroup.displayName = 'OptionsMenuItemGroup';
//# sourceMappingURL=OptionsMenuItemGroup.js.map