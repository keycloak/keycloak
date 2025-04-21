import * as React from 'react';
interface DraggableItemPosition {
    /** Parent droppableId */
    droppableId: string;
    /** Index of item in parent Droppable */
    index: number;
}
export declare const DragDropContext: React.Context<{
    onDrag: (_source: DraggableItemPosition) => boolean;
    onDragMove: (_source: DraggableItemPosition, _dest?: DraggableItemPosition) => void;
    onDrop: (_source: DraggableItemPosition, _dest?: DraggableItemPosition) => boolean;
}>;
interface DragDropProps {
    /** Potentially Droppable and Draggable children */
    children?: React.ReactNode;
    /** Callback for drag event. Return true to allow drag, false to disallow. */
    onDrag?: (source: DraggableItemPosition) => boolean;
    /** Callback on mouse move while dragging. */
    onDragMove?: (source: DraggableItemPosition, dest?: DraggableItemPosition) => void;
    /** Callback for drop event. Return true to allow drop, false to disallow. */
    onDrop?: (source: DraggableItemPosition, dest?: DraggableItemPosition) => boolean;
}
export declare const DragDrop: React.FunctionComponent<DragDropProps>;
export {};
//# sourceMappingURL=DragDrop.d.ts.map