"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const DataListContent = (_a) => {
    var { className = '', children = null, id = '', isHidden = false, 'aria-label': ariaLabel, hasNoPadding = false, 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    rowid = '' } = _a, props = tslib_1.__rest(_a, ["className", "children", "id", "isHidden", 'aria-label', "hasNoPadding", "rowid"]);
    return (React.createElement("section", Object.assign({ id: id, className: react_styles_1.css(data_list_1.default.dataListExpandableContent, className), hidden: isHidden, "aria-label": ariaLabel }, props),
        React.createElement("div", { className: react_styles_1.css(data_list_1.default.dataListExpandableContentBody, hasNoPadding && data_list_1.default.modifiers.noPadding) }, children)));
};
exports.DataListContent = DataListContent;
exports.DataListContent.displayName = 'DataListContent';
//# sourceMappingURL=DataListContent.js.map