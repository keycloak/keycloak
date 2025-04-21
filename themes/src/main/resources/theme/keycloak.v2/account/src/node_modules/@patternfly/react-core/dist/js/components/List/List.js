"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.List = exports.ListComponent = exports.ListVariant = exports.OrderType = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/List/list"));
const react_styles_1 = require("@patternfly/react-styles");
var OrderType;
(function (OrderType) {
    OrderType["number"] = "1";
    OrderType["lowercaseLetter"] = "a";
    OrderType["uppercaseLetter"] = "A";
    OrderType["lowercaseRomanNumber"] = "i";
    OrderType["uppercaseRomanNumber"] = "I";
})(OrderType = exports.OrderType || (exports.OrderType = {}));
var ListVariant;
(function (ListVariant) {
    ListVariant["inline"] = "inline";
})(ListVariant = exports.ListVariant || (exports.ListVariant = {}));
var ListComponent;
(function (ListComponent) {
    ListComponent["ol"] = "ol";
    ListComponent["ul"] = "ul";
})(ListComponent = exports.ListComponent || (exports.ListComponent = {}));
const List = (_a) => {
    var { className = '', children = null, variant = null, isBordered = false, isPlain = false, iconSize = 'default', type = OrderType.number, ref = null, component = ListComponent.ul } = _a, props = tslib_1.__rest(_a, ["className", "children", "variant", "isBordered", "isPlain", "iconSize", "type", "ref", "component"]);
    return component === ListComponent.ol ? (React.createElement("ol", Object.assign({ ref: ref, type: type }, props, { className: react_styles_1.css(list_1.default.list, variant && list_1.default.modifiers[variant], isBordered && list_1.default.modifiers.bordered, isPlain && list_1.default.modifiers.plain, iconSize && iconSize === 'large' && list_1.default.modifiers.iconLg, className) }), children)) : (React.createElement("ul", Object.assign({ ref: ref }, props, { className: react_styles_1.css(list_1.default.list, variant && list_1.default.modifiers[variant], isBordered && list_1.default.modifiers.bordered, isPlain && list_1.default.modifiers.plain, iconSize && iconSize === 'large' && list_1.default.modifiers.iconLg, className) }), children));
};
exports.List = List;
exports.List.displayName = 'List';
//# sourceMappingURL=List.js.map