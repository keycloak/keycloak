import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export declare enum DataListWrapModifier {
    nowrap = "nowrap",
    truncate = "truncate",
    breakWord = "breakWord"
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
export declare const DataListContext: React.Context<Partial<DataListContextProps>>;
export declare class DataList extends React.Component<DataListProps, DataListState> {
    static displayName: string;
    static defaultProps: PickOptional<DataListProps>;
    dragFinished: boolean;
    html5DragDrop: boolean;
    arrayCopy: React.ReactElement[];
    ref: React.RefObject<HTMLUListElement>;
    state: DataListState;
    constructor(props: DataListProps);
    componentDidUpdate(oldProps: DataListProps): void;
    getIndex: (id: string) => number;
    move: (itemOrder: string[]) => void;
    dragStart0: (el: HTMLElement) => void;
    dragStart: (evt: React.DragEvent) => void;
    onDragCancel: () => void;
    dragLeave: (evt: React.DragEvent) => void;
    dragEnd0: (el: HTMLElement) => void;
    dragEnd: (evt: React.DragEvent) => void;
    isValidDrop: (evt: React.DragEvent) => boolean;
    drop: (evt: React.DragEvent) => void;
    dragOver0: (id: string) => void;
    dragOver: (evt: React.DragEvent) => string | null;
    handleDragButtonKeys: (evt: React.KeyboardEvent) => void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=DataList.d.ts.map