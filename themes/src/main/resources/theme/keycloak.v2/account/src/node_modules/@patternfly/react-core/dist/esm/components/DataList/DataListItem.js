import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { DataListContext } from './DataList';
import { KeyTypes } from '../../helpers/constants';
import { DataListDragButton } from './DataListDragButton';
function findDataListDragButton(node) {
    if (!React.isValidElement(node)) {
        return null;
    }
    if (node.type === DataListDragButton) {
        return node;
    }
    if (node.props.children) {
        for (const child of React.Children.toArray(node.props.children)) {
            const button = findDataListDragButton(child);
            if (button) {
                return button;
            }
        }
    }
    return null;
}
export class DataListItem extends React.Component {
    render() {
        const _a = this.props, { children, isExpanded, className, id, 'aria-labelledby': ariaLabelledBy, selectableInputAriaLabel } = _a, props = __rest(_a, ["children", "isExpanded", "className", "id", 'aria-labelledby', "selectableInputAriaLabel"]);
        return (React.createElement(DataListContext.Consumer, null, ({ isSelectable, selectedDataListItemId, updateSelectedDataListItem, selectableRow, isDraggable, dragStart, dragEnd, drop }) => {
            const selectDataListItem = (event) => {
                let target = event.target;
                while (event.currentTarget !== target) {
                    if (('onclick' in target && target.onclick) ||
                        target.parentNode.classList.contains(styles.dataListItemAction) ||
                        target.parentNode.classList.contains(styles.dataListItemControl)) {
                        // check other event handlers are not present.
                        return;
                    }
                    else {
                        target = target.parentNode;
                    }
                }
                updateSelectedDataListItem(id);
            };
            const onKeyDown = (event) => {
                if (event.key === KeyTypes.Enter) {
                    updateSelectedDataListItem(id);
                }
            };
            // We made the DataListDragButton determine if the entire item is draggable instead of
            // DataListItem like we should have.
            // Recursively search children for the DataListDragButton and see if it's disabled...
            const dragButton = findDataListDragButton(children);
            const dragProps = isDraggable && {
                draggable: dragButton ? !dragButton.props.isDisabled : true,
                onDrop: drop,
                onDragEnd: dragEnd,
                onDragStart: dragStart
            };
            const isSelected = selectedDataListItemId === id;
            const selectableInputAriaProps = selectableInputAriaLabel
                ? { 'aria-label': selectableInputAriaLabel }
                : { 'aria-labelledby': ariaLabelledBy };
            const selectableInputType = (selectableRow === null || selectableRow === void 0 ? void 0 : selectableRow.type) === 'multiple' ? 'checkbox' : 'radio';
            return (React.createElement("li", Object.assign({ id: id, className: css(styles.dataListItem, isExpanded && styles.modifiers.expanded, isSelectable && styles.modifiers.selectable, selectedDataListItemId && isSelected && styles.modifiers.selected, className), "aria-labelledby": ariaLabelledBy }, (isSelectable && { tabIndex: 0, onClick: selectDataListItem, onKeyDown }), (isSelectable && isSelected && { 'aria-selected': true }), props, dragProps),
                selectableRow && (React.createElement("input", Object.assign({ className: "pf-screen-reader", type: selectableInputType, checked: isSelected, onChange: event => selectableRow.onChange(id, event), tabIndex: -1 }, selectableInputAriaProps))),
                React.Children.map(children, child => React.isValidElement(child) &&
                    React.cloneElement(child, {
                        rowid: ariaLabelledBy
                    }))));
        }));
    }
}
DataListItem.displayName = 'DataListItem';
DataListItem.defaultProps = {
    isExpanded: false,
    className: '',
    id: '',
    children: null,
    'aria-labelledby': ''
};
//# sourceMappingURL=DataListItem.js.map