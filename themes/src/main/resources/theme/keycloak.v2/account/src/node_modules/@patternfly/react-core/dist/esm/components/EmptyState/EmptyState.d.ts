import * as React from 'react';
export declare enum EmptyStateVariant {
    'xs' = "xs",
    small = "small",
    large = "large",
    'xl' = "xl",
    full = "full"
}
export interface EmptyStateProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the EmptyState */
    className?: string;
    /** Content rendered inside the EmptyState */
    children: React.ReactNode;
    /** Modifies EmptyState max-width */
    variant?: 'xs' | 'small' | 'large' | 'xl' | 'full';
    /** Cause component to consume the available height of its container */
    isFullHeight?: boolean;
}
export declare const EmptyState: React.FunctionComponent<EmptyStateProps>;
//# sourceMappingURL=EmptyState.d.ts.map