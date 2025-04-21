import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListCheck = (_a) => {
    var { className = '', 
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    onChange = (checked, event) => { }, isValid = true, isDisabled = false, isChecked = null, checked = null, defaultChecked, otherControls = false } = _a, props = __rest(_a, ["className", "onChange", "isValid", "isDisabled", "isChecked", "checked", "defaultChecked", "otherControls"]);
    const check = (React.createElement("div", { className: css(styles.dataListCheck) },
        React.createElement("input", Object.assign({}, props, { type: "checkbox", onChange: event => onChange(event.currentTarget.checked, event), "aria-invalid": !isValid, disabled: isDisabled }, ([true, false].includes(defaultChecked) && { defaultChecked }), (![true, false].includes(defaultChecked) && { checked: isChecked || checked })))));
    return (React.createElement(React.Fragment, null,
        !otherControls && React.createElement("div", { className: css(styles.dataListItemControl, className) }, check),
        otherControls && check));
};
DataListCheck.displayName = 'DataListCheck';
//# sourceMappingURL=DataListCheck.js.map