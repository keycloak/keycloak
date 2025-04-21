"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownSeparator = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dropdownConstants_1 = require("./dropdownConstants");
const InternalDropdownItem_1 = require("./InternalDropdownItem");
const Divider_1 = require("../Divider");
const helpers_1 = require("../../helpers");
const DropdownSeparator = (_a) => {
    var { className = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref, // Types of Ref are different for React.FunctionComponent vs React.Component
    ouiaId, ouiaSafe } = _a, props = tslib_1.__rest(_a, ["className", "ref", "ouiaId", "ouiaSafe"]);
    const ouiaProps = helpers_1.useOUIAProps(exports.DropdownSeparator.displayName, ouiaId, ouiaSafe);
    return (React.createElement(dropdownConstants_1.DropdownArrowContext.Consumer, null, context => (React.createElement(InternalDropdownItem_1.InternalDropdownItem, Object.assign({}, props, { context: context, component: React.createElement(Divider_1.Divider, { component: Divider_1.DividerVariant.div }), className: className, role: "separator" }, ouiaProps)))));
};
exports.DropdownSeparator = DropdownSeparator;
exports.DropdownSeparator.displayName = 'DropdownSeparator';
//# sourceMappingURL=DropdownSeparator.js.map