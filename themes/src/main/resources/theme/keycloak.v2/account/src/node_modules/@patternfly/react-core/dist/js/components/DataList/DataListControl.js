"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListControl = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const DataListControl = (_a) => {
    var { children, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(data_list_1.default.dataListItemControl, className) }, props), children));
};
exports.DataListControl = DataListControl;
exports.DataListControl.displayName = 'DataListControl';
//# sourceMappingURL=DataListControl.js.map