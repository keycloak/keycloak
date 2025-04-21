"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SelectGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const select_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Select/select"));
const react_styles_1 = require("@patternfly/react-styles");
const selectConstants_1 = require("./selectConstants");
const SelectGroup = (_a) => {
    var { children = [], className = '', label = '', titleId = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "label", "titleId"]);
    return (React.createElement(selectConstants_1.SelectConsumer, null, ({ variant }) => (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(select_1.default.selectMenuGroup, className) }),
        React.createElement("div", { className: react_styles_1.css(select_1.default.selectMenuGroupTitle), id: titleId, "aria-hidden": true }, label),
        variant === selectConstants_1.SelectVariant.checkbox ? children : React.createElement("ul", { role: "listbox" }, children)))));
};
exports.SelectGroup = SelectGroup;
exports.SelectGroup.displayName = 'SelectGroup';
//# sourceMappingURL=SelectGroup.js.map