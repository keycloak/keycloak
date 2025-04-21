import * as React from 'react';
interface DroppableProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside DragDrop */
    children?: React.ReactNode;
    /** Class to add to outer div */
    className?: string;
    /** Name of zone that items can be dragged between. Should specify if there is more than one Droppable on the page. */
    zone?: string;
    /** Id to be passed back on drop events */
    droppableId?: string;
    /** Don't wrap the component in a div. Requires passing a single child. */
    hasNoWrapper?: boolean;
}
export declare const Droppable: React.FunctionComponent<DroppableProps>;
export {};
//# sourceMappingURL=Droppable.d.ts.map