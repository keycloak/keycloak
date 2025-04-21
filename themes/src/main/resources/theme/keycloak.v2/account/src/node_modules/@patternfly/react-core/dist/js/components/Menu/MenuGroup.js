"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
const MenuGroupBase = (_a) => {
    var { children, className = '', label = '', titleId = '', innerRef } = _a, props = tslib_1.__rest(_a, ["children", "className", "label", "titleId", "innerRef"]);
    return (React.createElement("section", Object.assign({}, props, { className: react_styles_1.css('pf-c-menu__group', className), ref: innerRef }),
        label && (React.createElement("h1", { className: react_styles_1.css(menu_1.default.menuGroupTitle), id: titleId }, label)),
        children));
};
exports.MenuGroup = React.forwardRef((props, ref) => (React.createElement(MenuGroupBase, Object.assign({}, props, { innerRef: ref }))));
exports.MenuGroup.displayName = 'MenuGroup';
//# sourceMappingURL=MenuGroup.js.map