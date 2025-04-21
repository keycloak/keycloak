"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListCell = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const DataListCell = (_a) => {
    var { children = null, className = '', width = 1, isFilled = true, alignRight = false, isIcon = false, wrapModifier = null } = _a, props = tslib_1.__rest(_a, ["children", "className", "width", "isFilled", "alignRight", "isIcon", "wrapModifier"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(data_list_1.default.dataListCell, width > 1 && data_list_1.default.modifiers[`flex_${width}`], !isFilled && data_list_1.default.modifiers.noFill, alignRight && data_list_1.default.modifiers.alignRight, isIcon && data_list_1.default.modifiers.icon, className, wrapModifier && data_list_1.default.modifiers[wrapModifier]) }, props), children));
};
exports.DataListCell = DataListCell;
exports.DataListCell.displayName = 'DataListCell';
//# sourceMappingURL=DataListCell.js.map