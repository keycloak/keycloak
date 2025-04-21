import * as React from 'react';
export interface CardActionsProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Card Action */
    children?: React.ReactNode;
    /** Additional classes added to the Action */
    className?: string;
    /** Flag indicating that the actions have no offset */
    hasNoOffset?: boolean;
}
export declare const CardActions: React.FunctionComponent<CardActionsProps>;
//# sourceMappingURL=CardActions.d.ts.map