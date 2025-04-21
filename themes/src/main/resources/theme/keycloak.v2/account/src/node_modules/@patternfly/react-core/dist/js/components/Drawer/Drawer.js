"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Drawer = exports.DrawerContext = exports.DrawerColorVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const drawer_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Drawer/drawer"));
const react_styles_1 = require("@patternfly/react-styles");
var DrawerColorVariant;
(function (DrawerColorVariant) {
    DrawerColorVariant["default"] = "default";
    DrawerColorVariant["light200"] = "light-200";
})(DrawerColorVariant = exports.DrawerColorVariant || (exports.DrawerColorVariant = {}));
exports.DrawerContext = React.createContext({
    isExpanded: false,
    isStatic: false,
    onExpand: () => { },
    position: 'right',
    drawerRef: null,
    drawerContentRef: null,
    isInline: false
});
const Drawer = (_a) => {
    var { className = '', children, isExpanded = false, isInline = false, isStatic = false, position = 'right', onExpand = () => { } } = _a, props = tslib_1.__rest(_a, ["className", "children", "isExpanded", "isInline", "isStatic", "position", "onExpand"]);
    const drawerRef = React.useRef();
    const drawerContentRef = React.useRef();
    return (React.createElement(exports.DrawerContext.Provider, { value: { isExpanded, isStatic, onExpand, position, drawerRef, drawerContentRef, isInline } },
        React.createElement("div", Object.assign({ className: react_styles_1.css(drawer_1.default.drawer, isExpanded && drawer_1.default.modifiers.expanded, isInline && drawer_1.default.modifiers.inline, isStatic && drawer_1.default.modifiers.static, position === 'left' && drawer_1.default.modifiers.panelLeft, position === 'bottom' && drawer_1.default.modifiers.panelBottom, className), ref: drawerRef }, props), children)));
};
exports.Drawer = Drawer;
exports.Drawer.displayName = 'Drawer';
//# sourceMappingURL=Drawer.js.map