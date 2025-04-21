"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListAction = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const util_1 = require("../../helpers/util");
const DataListAction = (_a) => {
    var { children, className, visibility, 
    /* eslint-disable @typescript-eslint/no-unused-vars */
    id, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, isPlainButtonAction } = _a, 
    /* eslint-enable @typescript-eslint/no-unused-vars */
    props = tslib_1.__rest(_a, ["children", "className", "visibility", "id", 'aria-label', 'aria-labelledby', "isPlainButtonAction"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(data_list_1.default.dataListItemAction, util_1.formatBreakpointMods(visibility, data_list_1.default), className) }, props), isPlainButtonAction ? React.createElement("div", { className: react_styles_1.css(data_list_1.default.dataListAction) }, children) : children));
};
exports.DataListAction = DataListAction;
exports.DataListAction.displayName = 'DataListAction';
//# sourceMappingURL=DataListAction.js.map