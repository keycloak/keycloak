"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DrawerCloseButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));
const react_styles_1 = require("@patternfly/react-styles");
const Button_1 = require("../Button");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const DrawerCloseButton = (_a) => {
    var { 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    className = '', onClose = () => undefined, 'aria-label': ariaLabel = 'Close drawer panel' } = _a, props = tslib_1.__rest(_a, ["className", "onClose", 'aria-label']);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(drawer_1.default.drawerClose, className) }, props),
        React.createElement(Button_1.Button, { variant: "plain", onClick: onClose, "aria-label": ariaLabel },
            React.createElement(times_icon_1.default, null))));
};
exports.DrawerCloseButton = DrawerCloseButton;
exports.DrawerCloseButton.displayName = 'DrawerCloseButton';
//# sourceMappingURL=DrawerCloseButton.js.map