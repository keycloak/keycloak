"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MenuInput = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Menu/menu"));
const react_styles_1 = require("@patternfly/react-styles");
exports.MenuInput = React.forwardRef((props, ref) => (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(menu_1.default.menuSearch, props.className), ref: ref }))));
exports.MenuInput.displayName = 'MenuInput';
//# sourceMappingURL=MenuInput.js.map