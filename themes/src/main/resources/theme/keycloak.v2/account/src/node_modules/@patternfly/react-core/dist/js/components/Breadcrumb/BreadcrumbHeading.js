"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BreadcrumbHeading = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const breadcrumb_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Breadcrumb/breadcrumb"));
const react_styles_1 = require("@patternfly/react-styles");
const BreadcrumbHeading = (_a) => {
    var { children = null, className = '', to = undefined, target = undefined, component = 'a', showDivider } = _a, props = tslib_1.__rest(_a, ["children", "className", "to", "target", "component", "showDivider"]);
    const Component = component;
    return (React.createElement("li", Object.assign({}, props, { className: react_styles_1.css(breadcrumb_1.default.breadcrumbItem, className) }),
        showDivider && (React.createElement("span", { className: breadcrumb_1.default.breadcrumbItemDivider },
            React.createElement(angle_right_icon_1.default, null))),
        React.createElement("h1", { className: breadcrumb_1.default.breadcrumbHeading },
            !to && component === 'button' && (React.createElement("button", { className: react_styles_1.css(breadcrumb_1.default.breadcrumbLink, breadcrumb_1.default.modifiers.current), "aria-current": true, type: "button" }, children)),
            to && (React.createElement(Component, { href: to, target: target, className: react_styles_1.css(breadcrumb_1.default.breadcrumbLink, breadcrumb_1.default.modifiers.current), "aria-current": "page" }, children)),
            !to && component !== 'button' && React.createElement(React.Fragment, null, children))));
};
exports.BreadcrumbHeading = BreadcrumbHeading;
exports.BreadcrumbHeading.displayName = 'BreadcrumbHeading';
//# sourceMappingURL=BreadcrumbHeading.js.map