"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const popover_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Popover/popover"));
const react_styles_1 = require("@patternfly/react-styles");
const PopoverBody = (_a) => {
    var { children, id, className } = _a, props = tslib_1.__rest(_a, ["children", "id", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(popover_1.default.popoverBody, className), id: id }, props), children));
};
exports.PopoverBody = PopoverBody;
exports.PopoverBody.displayName = 'PopoverBody';
//# sourceMappingURL=PopoverBody.js.map