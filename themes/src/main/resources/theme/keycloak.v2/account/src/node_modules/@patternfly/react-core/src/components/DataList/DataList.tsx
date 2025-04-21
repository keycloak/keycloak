import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import stylesGrid from '@patternfly/react-styles/css/components/DataList/data-list-grid';
import { PickOptional } from '../../helpers/typeUtils';

const gridBreakpointClasses = {
  none: stylesGrid.modifiers.gridNone,
  always: 'pf-m-grid', // Placeholder per https://github.com/patternfly/patternfly-react/issues/4965#issuecomment-704984236
  sm: stylesGrid.modifiers.gridSm,
  md: stylesGrid.modifiers.gridMd,
  lg: stylesGrid.modifiers.gridLg,
  xl: stylesGrid.modifiers.gridXl,
  '2xl': stylesGrid.modifiers.grid_2xl
};

export enum DataListWrapModifier {
  nowrap = 'nowrap',
  truncate = 'truncate',
  breakWord = 'breakWord'
}

export interface SelectableRowObject {
  /** Determines if only one of the selectable rows should be selectable at a time */
  type: 'multiple' | 'single';
  /** Callback that executes when the screen reader accessible element receives a change event */
  onChange: (id: string, event: React.FormEvent<HTMLInputElement>) => void;
}

export interface DataListProps extends Omit<React.HTMLProps<HTMLUListElement>, 'onDragStart' | 'ref'> {
  /** Content rendered inside the DataList list */
  children?: React.ReactNode;
  /** Additional classes added to the DataList list */
  className?: string;
  /** Adds accessible text to the DataList list */
  'aria-label': string;
  /** Optional callback to make DataList selectable, fired when DataListItem selected */
  onSelectDataListItem?: (id: string) => void;
  /** @deprecated Optional callback to make DataList draggable, fired when dragging ends */
  onDragFinish?: (newItemOrder: string[]) => void;
  /** @deprecated Optional informational callback for dragging, fired when dragging starts */
  onDragStart?: (id: string) => void;
  /** @deprecated Optional informational callback for dragging, fired when an item moves */
  onDragMove?: (oldIndex: number, newIndex: number) => void;
  /** @deprecated Optional informational callback for dragging, fired when dragging is cancelled */
  onDragCancel?: () => void;
  /** Id of DataList item currently selected */
  selectedDataListItemId?: string;
  /** Flag indicating if DataList should have compact styling */
  isCompact?: boolean;
  /** Specifies the grid breakpoints  */
  gridBreakpoint?: 'none' | 'always' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  /** Determines which wrapping modifier to apply to the DataList */
  wrapModifier?: DataListWrapModifier | 'nowrap' | 'truncate' | 'breakWord';
  /** @deprecated Order of items in a draggable DataList */
  itemOrder?: string[];
  /** @beta Object that causes the data list to render hidden inputs which improve selectable item a11y */
  selectableRow?: SelectableRowObject;
}

interface DataListState {
  draggedItemId: string;
  draggingToItemIndex: number;
  dragging: boolean;
  tempItemOrder: string[];
}

interface DataListContextProps {
  isSelectable: boolean;
  selectedDataListItemId: string;
  updateSelectedDataListItem: (id: string) => void;
  selectableRow?: SelectableRowObject;
  isDraggable: boolean;
  dragStart: (e: React.DragEvent) => void;
  dragEnd: (e: React.DragEvent) => void;
  drop: (e: React.DragEvent) => void;
  dragKeyHandler: (e: React.KeyboardEvent) => void;
}

export const DataListContext = React.createContext<Partial<DataListContextProps>>({
  isSelectable: false
});

const moveItem = (arr: string[], i1: string, toIndex: number) => {
  const fromIndex = arr.indexOf(i1);
  if (fromIndex === toIndex) {
    return arr;
  }
  const temp = arr.splice(fromIndex, 1);
  arr.splice(toIndex, 0, temp[0]);

  return arr;
};

export class DataList extends React.Component<DataListProps, DataListState> {
  static displayName = 'DataList';
  static defaultProps: PickOptional<DataListProps> = {
    children: null,
    className: '',
    selectedDataListItemId: '',
    isCompact: false,
    gridBreakpoint: 'md',
    wrapModifier: null
  };
  dragFinished: boolean = false;
  html5DragDrop: boolean = false;
  arrayCopy: React.ReactElement[] = React.Children.toArray(this.props.children) as React.ReactElement[];
  ref = React.createRef<HTMLUListElement>();

  state: DataListState = {
    tempItemOrder: [],
    draggedItemId: null,
    draggingToItemIndex: null,
    dragging: false
  };

  constructor(props: DataListProps) {
    super(props);

    this.html5DragDrop = Boolean(props.onDragFinish || props.onDragStart || props.onDragMove || props.onDragCancel);
    if (this.html5DragDrop) {
      // eslint-disable-next-line no-console
      console.warn("DataList's onDrag API is deprecated. Use DragDrop instead.");
    }
  }

  componentDidUpdate(oldProps: DataListProps) {
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

  getIndex = (id: string) => Array.from(this.ref.current.children).findIndex(item => item.id === id);

  move = (itemOrder: string[]) => {
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

  dragStart0 = (el: HTMLElement) => {
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

  dragStart = (evt: React.DragEvent) => {
    evt.dataTransfer.effectAllowed = 'move';
    evt.dataTransfer.setData('text/plain', evt.currentTarget.id);
    this.dragStart0(evt.currentTarget as HTMLElement);
  };

  onDragCancel = () => {
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

  dragLeave = (evt: React.DragEvent) => {
    // This event false fires when we call `this.move()`, so double check we're out of zone
    if (!this.isValidDrop(evt)) {
      this.move(this.props.itemOrder);
      this.setState({
        draggingToItemIndex: null
      });
    }
  };

  dragEnd0 = (el: HTMLElement) => {
    el.classList.remove(styles.modifiers.ghostRow);
    el.classList.remove(styles.modifiers.dragOver);
    el.setAttribute('aria-pressed', 'false');
    this.setState({
      draggedItemId: null,
      draggingToItemIndex: null,
      dragging: false
    });
  };

  dragEnd = (evt: React.DragEvent) => {
    this.dragEnd0(evt.target as HTMLElement);
  };

  isValidDrop = (evt: React.DragEvent) => {
    const ulRect = this.ref.current.getBoundingClientRect();
    return (
      evt.clientX > ulRect.x &&
      evt.clientX < ulRect.x + ulRect.width &&
      evt.clientY > ulRect.y &&
      evt.clientY < ulRect.y + ulRect.height
    );
  };

  drop = (evt: React.DragEvent) => {
    if (this.isValidDrop(evt)) {
      this.props.onDragFinish(this.state.tempItemOrder);
    } else {
      this.onDragCancel();
    }
  };

  dragOver0 = (id: string) => {
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

  dragOver = (evt: React.DragEvent): string | null => {
    evt.preventDefault();

    const curListItem = (evt.target as HTMLElement).closest('li');
    if (!curListItem || !this.ref.current.contains(curListItem) || curListItem.id === this.state.draggedItemId) {
      // We're going nowhere, don't bother calling `dragOver0`
      return null;
    } else {
      this.dragOver0(curListItem.id);
    }
  };

  handleDragButtonKeys = (evt: React.KeyboardEvent) => {
    const { dragging } = this.state;
    if (![' ', 'Escape', 'Enter', 'ArrowUp', 'ArrowDown'].includes(evt.key) || !this.html5DragDrop) {
      if (dragging) {
        evt.preventDefault();
      }
      return;
    }
    evt.preventDefault();

    const dragItem = (evt.target as Element).closest('li');

    if (evt.key === ' ' || (evt.key === 'Enter' && !dragging)) {
      this.dragStart0(dragItem);
    } else if (dragging) {
      if (evt.key === 'Escape' || evt.key === 'Enter') {
        this.setState({
          dragging: false
        });
        this.dragFinished = true;
        if (evt.key === 'Enter') {
          this.dragEnd0(dragItem);
          this.props.onDragFinish(this.state.tempItemOrder);
        } else {
          this.onDragCancel();
        }
      } else if (evt.key === 'ArrowUp') {
        const nextSelection = dragItem.previousSibling as HTMLElement;
        if (nextSelection) {
          this.dragOver0(nextSelection.id);
          (dragItem.querySelector(`.${styles.dataListItemDraggableButton}`) as HTMLElement).focus();
        }
      } else if (evt.key === 'ArrowDown') {
        const nextSelection = dragItem.nextSibling as HTMLElement;
        if (nextSelection) {
          this.dragOver0(nextSelection.id);
          (dragItem.querySelector(`.${styles.dataListItemDraggableButton}`) as HTMLElement).focus();
        }
      }
    }
  };

  render() {
    const {
      className,
      children,
      onSelectDataListItem,
      selectedDataListItemId,
      isCompact,
      wrapModifier,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      onDragStart,
      onDragMove,
      onDragCancel,
      onDragFinish,
      gridBreakpoint,
      itemOrder,
      selectableRow,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      ...props
    } = this.props;
    const { dragging } = this.state;
    const isSelectable = onSelectDataListItem !== undefined;

    const updateSelectedDataListItem = (id: string) => {
      onSelectDataListItem(id);
    };

    const dragProps = this.html5DragDrop && {
      onDragOver: this.dragOver,
      onDrop: this.dragOver,
      onDragLeave: this.dragLeave
    };

    return (
      <DataListContext.Provider
        value={{
          isSelectable,
          selectedDataListItemId,
          updateSelectedDataListItem,
          selectableRow,
          isDraggable: this.html5DragDrop,
          dragStart: this.dragStart,
          dragEnd: this.dragEnd,
          drop: this.drop,
          dragKeyHandler: this.handleDragButtonKeys
        }}
      >
        <ul
          className={css(
            styles.dataList,
            isCompact && styles.modifiers.compact,
            gridBreakpointClasses[gridBreakpoint],
            wrapModifier && styles.modifiers[wrapModifier],
            dragging && styles.modifiers.dragOver,
            className
          )}
          style={props.style}
          {...props}
          {...dragProps}
          ref={this.ref}
        >
          {children}
        </ul>
      </DataListContext.Provider>
    );
  }
}
