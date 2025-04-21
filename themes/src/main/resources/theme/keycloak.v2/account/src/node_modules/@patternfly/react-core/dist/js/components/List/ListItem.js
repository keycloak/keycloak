"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ListItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/List/list"));
const react_styles_1 = require("@patternfly/react-styles");
const ListItem = (_a) => {
    var { icon = null, children = null } = _a, props = tslib_1.__rest(_a, ["icon", "children"]);
    return (React.createElement("li", Object.assign({ className: react_styles_1.css(icon && list_1.default.listItem) }, props),
        icon && React.createElement("span", { className: react_styles_1.css(list_1.default.listItemIcon) }, icon),
        children));
};
exports.ListItem = ListItem;
exports.ListItem.displayName = 'ListItem';
//# sourceMappingURL=ListItem.js.map