import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import stylesGrid from '@patternfly/react-styles/css/components/DataList/data-list-grid';
const gridBreakpointClasses = {
    none: stylesGrid.modifiers.gridNone,
    always: 'pf-m-grid',
    sm: stylesGrid.modifiers.gridSm,
    md: stylesGrid.modifiers.gridMd,
    lg: stylesGrid.modifiers.gridLg,
    xl: stylesGrid.modifiers.gridXl,
    '2xl': stylesGrid.modifiers.grid_2xl
};
export var DataListWrapModifier;
(function (DataListWrapModifier) {
    DataListWrapModifier["nowrap"] = "nowrap";
    DataListWrapModifier["truncate"] = "truncate";
    DataListWrapModifier["breakWord"] = "breakWord";
})(DataListWrapModifier || (DataListWrapModifier = {}));
export const DataListContext = React.createContext({
    isSelectable: false
});
const moveItem = (arr, i1, toIndex) => {
    const fromIndex = arr.indexOf(i1);
    if (fromIndex === toIndex) {
        return arr;
    }
    const temp = arr.splice(fromIndex, 1);
    arr.splice(toIndex, 0, temp[0]);
    return arr;
};
export class DataList extends React.Component {
    constructor(props) {
        super(props);
        this.dragFinished = false;
        this.html5DragDrop = false;
        this.arrayCopy = React.Children.toArray(this.props.children);
        this.ref = React.createRef();
        this.state = {
            tempItemOrder: [],
            draggedItemId: null,
            draggingToItemIndex: null,
            dragging: false
        };
        this.getIndex = (id) => Array.from(this.ref.current.children).findIndex(item => item.id === id);
        this.move = (itemOrder) => {
            const ulNode = this.ref.current;
            const nodes = Array.from(ulNode.children);
            if (nodes.map(node => node.id).every((id, i) => id === itemOrder[i])) {
                return;
            }
            while (ulNode.firstChild) {
                ulNode.removeChild(ulNode.lastChild);
            }
            itemOrder.forEach(id => {
                ulNode.appendChild(nodes.find(n => n.id === id));
            });
        };
        this.dragStart0 = (el) => {
            const { onDragStart } = this.props;
            const draggedItemId = el.id;
            el.classList.add(styles.modifiers.ghostRow);
            el.setAttribute('aria-pressed', 'true');
            this.setState({
                draggedItemId,
                dragging: true
            });
            onDragStart && onDragStart(draggedItemId);
        };
        this.dragStart = (evt) => {
            evt.dataTransfer.effectAllowed = 'move';
            evt.dataTransfer.setData('text/plain', evt.currentTarget.id);
            this.dragStart0(evt.currentTarget);
        };
        this.onDragCancel = () => {
            this.move(this.props.itemOrder);
            Array.from(this.ref.current.children).forEach(el => {
                el.classList.remove(styles.modifiers.ghostRow);
                el.classList.remove(styles.modifiers.dragOver);
                el.setAttribute('aria-pressed', 'false');
            });
            this.setState({
                draggedItemId: null,
                draggingToItemIndex: null,
                dragging: false
            });
            if (this.props.onDragCancel) {
                this.props.onDragCancel();
            }
        };
        this.dragLeave = (evt) => {
            // This event false fires when we call `this.move()`, so double check we're out of zone
            if (!this.isValidDrop(evt)) {
                this.move(this.props.itemOrder);
                this.setState({
                    draggingToItemIndex: null
                });
            }
        };
        this.dragEnd0 = (el) => {
            el.classList.remove(styles.modifiers.ghostRow);
            el.classList.remove(styles.modifiers.dragOver);
            el.setAttribute('aria-pressed', 'false');
            this.setState({
                draggedItemId: null,
                draggingToItemIndex: null,
                dragging: false
            });
        };
        this.dragEnd = (evt) => {
            this.dragEnd0(evt.target);
        };
        this.isValidDrop = (evt) => {
            const ulRect = this.ref.current.getBoundingClientRect();
            return (evt.clientX > ulRect.x &&
                evt.clientX < ulRect.x + ulRect.width &&
                evt.clientY > ulRect.y &&
                evt.clientY < ulRect.y + ulRect.height);
        };
        this.drop = (evt) => {
            if (this.isValidDrop(evt)) {
                this.props.onDragFinish(this.state.tempItemOrder);
            }
            else {
                this.onDragCancel();
            }
        };
        this.dragOver0 = (id) => {
            const draggingToItemIndex = Array.from(this.ref.current.children).findIndex(item => item.id === id);
            if (draggingToItemIndex !== this.state.draggingToItemIndex) {
                const tempItemOrder = moveItem([...this.props.itemOrder], this.state.draggedItemId, draggingToItemIndex);
                this.move(tempItemOrder);
                this.setState({
                    draggingToItemIndex,
                    tempItemOrder
                });
            }
        };
        this.dragOver = (evt) => {
            evt.preventDefault();
            const curListItem = evt.target.closest('li');
            if (!curListItem || !this.ref.current.contains(curListItem) || curListItem.id === this.state.draggedItemId) {
                // We're going nowhere, don't bother calling `dragOver0`
                return null;
            }
            else {
                this.dragOver0(curListItem.id);
            }
        };
        this.handleDragButtonKeys = (evt) => {
            const { dragging } = this.state;
            if (![' ', 'Escape', 'Enter', 'ArrowUp', 'ArrowDown'].includes(evt.key) || !this.html5DragDrop) {
                if (dragging) {
                    evt.preventDefault();
                }
                return;
            }
            evt.preventDefault();
            const dragItem = evt.target.closest('li');
            if (evt.key === ' ' || (evt.key === 'Enter' && !dragging)) {
                this.dragStart0(dragItem);
            }
            else if (dragging) {
                if (evt.key === 'Escape' || evt.key === 'Enter') {
                    this.setState({
                        dragging: false
                    });
                    this.dragFinished = true;
                    if (evt.key === 'Enter') {
                        this.dragEnd0(dragItem);
                        this.props.onDragFinish(this.state.tempItemOrder);
                    }
                    else {
                        this.onDragCancel();
                    }
                }
                else if (evt.key === 'ArrowUp') {
                    const nextSelection = dragItem.previousSibling;
                    if (nextSelection) {
                        this.dragOver0(nextSelection.id);
                        dragItem.querySelector(`.${styles.dataListItemDraggableButton}`).focus();
                    }
                }
                else if (evt.key === 'ArrowDown') {
                    const nextSelection = dragItem.nextSibling;
                    if (nextSelection) {
                        this.dragOver0(nextSelection.id);
                        dragItem.querySelector(`.${styles.dataListItemDraggableButton}`).focus();
                    }
                }
            }
        };
        this.html5DragDrop = Boolean(props.onDragFinish || props.onDragStart || props.onDragMove || props.onDragCancel);
        if (this.html5DragDrop) {
            // eslint-disable-next-line no-console
            console.warn("DataList's onDrag API is deprecated. Use DragDrop instead.");
        }
    }
    componentDidUpdate(oldProps) {
        if (this.dragFinished) {
            this.dragFinished = false;
            this.setState({
                tempItemOrder: [...this.props.itemOrder],
                draggedItemId: null,
                dragging: false
            });
        }
        if (oldProps.itemOrder !== this.props.itemOrder) {
            this.move(this.props.itemOrder);
        }
    }
    render() {
        const _a = this.props, { className, children, onSelectDataListItem, selectedDataListItemId, isCompact, wrapModifier, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onDragStart, onDragMove, onDragCancel, onDragFinish, gridBreakpoint, itemOrder, selectableRow } = _a, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        props = __rest(_a, ["className", "children", "onSelectDataListItem", "selectedDataListItemId", "isCompact", "wrapModifier", "onDragStart", "onDragMove", "onDragCancel", "onDragFinish", "gridBreakpoint", "itemOrder", "selectableRow"]);
        const { dragging } = this.state;
        const isSelectable = onSelectDataListItem !== undefined;
        const updateSelectedDataListItem = (id) => {
            onSelectDataListItem(id);
        };
        const dragProps = this.html5DragDrop && {
            onDragOver: this.dragOver,
            onDrop: this.dragOver,
            onDragLeave: this.dragLeave
        };
        return (React.createElement(DataListContext.Provider, { value: {
                isSelectable,
                selectedDataListItemId,
                updateSelectedDataListItem,
                selectableRow,
                isDraggable: this.html5DragDrop,
                dragStart: this.dragStart,
                dragEnd: this.dragEnd,
                drop: this.drop,
                dragKeyHandler: this.handleDragButtonKeys
            } },
            React.createElement("ul", Object.assign({ className: css(styles.dataList, isCompact && styles.modifiers.compact, gridBreakpointClasses[gridBreakpoint], wrapModifier && styles.modifiers[wrapModifier], dragging && styles.modifiers.dragOver, className), style: props.style }, props, dragProps, { ref: this.ref }), children)));
    }
}
DataList.displayName = 'DataList';
DataList.defaultProps = {
    children: null,
    className: '',
    selectedDataListItemId: '',
    isCompact: false,
    gridBreakpoint: 'md',
    wrapModifier: null
};
//# sourceMappingURL=DataList.js.map