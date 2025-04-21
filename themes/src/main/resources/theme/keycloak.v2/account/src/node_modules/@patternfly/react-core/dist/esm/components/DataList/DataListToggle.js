import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { Button, ButtonVariant } from '../Button';
export const DataListToggle = (_a) => {
    var { className = '', isExpanded = false, 'aria-controls': ariaControls = '', 'aria-label': ariaLabel = 'Details', rowid = '', id } = _a, props = __rest(_a, ["className", "isExpanded", 'aria-controls', 'aria-label', "rowid", "id"]);
    return (React.createElement("div", Object.assign({ className: css(styles.dataListItemControl, className) }, props),
        React.createElement("div", { className: css(styles.dataListToggle) },
            React.createElement(Button, { id: id, variant: ButtonVariant.plain, "aria-controls": ariaControls !== '' && ariaControls, "aria-label": ariaLabel, "aria-labelledby": ariaLabel !== 'Details' ? null : `${rowid} ${id}`, "aria-expanded": isExpanded },
                React.createElement("div", { className: css(styles.dataListToggleIcon) },
                    React.createElement(AngleRightIcon, null))))));
};
DataListToggle.displayName = 'DataListToggle';
//# sourceMappingURL=DataListToggle.js.map