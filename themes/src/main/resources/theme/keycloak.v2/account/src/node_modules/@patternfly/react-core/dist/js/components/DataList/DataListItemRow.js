"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListItemRow = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const DataListItemRow = (_a) => {
    var { children, className = '', rowid = '', wrapModifier = null } = _a, props = tslib_1.__rest(_a, ["children", "className", "rowid", "wrapModifier"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(data_list_1.default.dataListItemRow, className, wrapModifier && data_list_1.default.modifiers[wrapModifier]) }, props), React.Children.map(children, child => React.isValidElement(child) &&
        React.cloneElement(child, {
            rowid
        }))));
};
exports.DataListItemRow = DataListItemRow;
exports.DataListItemRow.displayName = 'DataListItemRow';
//# sourceMappingURL=DataListItemRow.js.map