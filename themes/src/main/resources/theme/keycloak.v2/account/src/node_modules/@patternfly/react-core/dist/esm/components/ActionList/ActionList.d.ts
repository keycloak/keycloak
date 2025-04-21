import * as React from 'react';
export interface ActionListProps extends React.HTMLProps<HTMLDivElement> {
    /** Children of the action list */
    children?: React.ReactNode;
    /** Flag indicating the action list contains multiple icons and item padding should be removed */
    isIconList?: boolean;
    /** Additional classes added to the action list */
    className?: string;
}
export declare const ActionList: React.FunctionComponent<ActionListProps>;
//# sourceMappingURL=ActionList.d.ts.map