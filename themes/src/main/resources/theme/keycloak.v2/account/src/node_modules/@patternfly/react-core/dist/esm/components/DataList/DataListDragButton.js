import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import GripVerticalIcon from '@patternfly/react-icons/dist/esm/icons/grip-vertical-icon';
import { DataListContext } from './DataList';
export const DataListDragButton = (_a) => {
    var { className = '', isDisabled = false } = _a, props = __rest(_a, ["className", "isDisabled"]);
    return (React.createElement(DataListContext.Consumer, null, ({ dragKeyHandler }) => (React.createElement("button", Object.assign({ className: css(styles.dataListItemDraggableButton, isDisabled && styles.modifiers.disabled, className), onKeyDown: dragKeyHandler, type: "button", disabled: isDisabled }, props),
        React.createElement("span", { className: css(styles.dataListItemDraggableIcon) },
            React.createElement(GripVerticalIcon, null))))));
};
DataListDragButton.displayName = 'DataListDragButton';
//# sourceMappingURL=DataListDragButton.js.map