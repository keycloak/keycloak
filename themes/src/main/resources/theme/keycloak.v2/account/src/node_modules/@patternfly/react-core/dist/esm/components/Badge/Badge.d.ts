import * as React from 'react';
export interface BadgeProps extends React.HTMLProps<HTMLSpanElement> {
    /**  Adds styling to the badge to indicate it has been read */
    isRead?: boolean;
    /** content rendered inside the Badge */
    children?: React.ReactNode;
    /** additional classes added to the Badge */
    className?: string;
}
export declare const Badge: React.FunctionComponent<BadgeProps>;
//# sourceMappingURL=Badge.d.ts.map