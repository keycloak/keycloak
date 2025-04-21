"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListCheck = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const DataListCheck = (_a) => {
    var { className = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onChange = (checked, event) => { }, isValid = true, isDisabled = false, isChecked = null, checked = null, defaultChecked, otherControls = false } = _a, props = tslib_1.__rest(_a, ["className", "onChange", "isValid", "isDisabled", "isChecked", "checked", "defaultChecked", "otherControls"]);
    const check = (React.createElement("div", { className: react_styles_1.css(data_list_1.default.dataListCheck) },
        React.createElement("input", Object.assign({}, props, { type: "checkbox", onChange: event => onChange(event.currentTarget.checked, event), "aria-invalid": !isValid, disabled: isDisabled }, ([true, false].includes(defaultChecked) && { defaultChecked }), (![true, false].includes(defaultChecked) && { checked: isChecked || checked })))));
    return (React.createElement(React.Fragment, null,
        !otherControls && React.createElement("div", { className: react_styles_1.css(data_list_1.default.dataListItemControl, className) }, check),
        otherControls && check));
};
exports.DataListCheck = DataListCheck;
exports.DataListCheck.displayName = 'DataListCheck';
//# sourceMappingURL=DataListCheck.js.map