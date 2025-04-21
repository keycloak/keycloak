import * as React from 'react';
export interface DraggableProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside DragDrop */
    children?: React.ReactNode;
    /** Don't wrap the component in a div. Requires passing a single child. */
    hasNoWrapper?: boolean;
    /** Class to add to outer div */
    className?: string;
}
export declare const Draggable: React.FunctionComponent<DraggableProps>;
//# sourceMappingURL=Draggable.d.ts.map