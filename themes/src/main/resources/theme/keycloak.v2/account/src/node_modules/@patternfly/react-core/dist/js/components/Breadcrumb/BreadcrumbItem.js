"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BreadcrumbItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const breadcrumb_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Breadcrumb/breadcrumb"));
const react_styles_1 = require("@patternfly/react-styles");
const BreadcrumbItem = (_a) => {
    var { children = null, className: classNameProp = '', to = undefined, isActive = false, isDropdown = false, showDivider, target = undefined, component = 'a', render = undefined } = _a, props = tslib_1.__rest(_a, ["children", "className", "to", "isActive", "isDropdown", "showDivider", "target", "component", "render"]);
    const Component = component;
    const ariaCurrent = isActive ? 'page' : undefined;
    const className = react_styles_1.css(breadcrumb_1.default.breadcrumbLink, isActive && breadcrumb_1.default.modifiers.current);
    return (React.createElement("li", Object.assign({}, props, { className: react_styles_1.css(breadcrumb_1.default.breadcrumbItem, classNameProp) }),
        showDivider && (React.createElement("span", { className: breadcrumb_1.default.breadcrumbItemDivider },
            React.createElement(angle_right_icon_1.default, null))),
        component === 'button' && (React.createElement("button", { className: className, "aria-current": ariaCurrent, type: "button" }, children)),
        isDropdown && React.createElement("span", { className: react_styles_1.css(breadcrumb_1.default.breadcrumbDropdown) }, children),
        render && render({ className, ariaCurrent }),
        to && !render && (React.createElement(Component, { href: to, target: target, className: className, "aria-current": ariaCurrent }, children)),
        !to && component !== 'button' && !isDropdown && children));
};
exports.BreadcrumbItem = BreadcrumbItem;
exports.BreadcrumbItem.displayName = 'BreadcrumbItem';
//# sourceMappingURL=BreadcrumbItem.js.map