import * as React from 'react';
export interface TabContentBodyProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the tab content body. */
    children: React.ReactNode;
    /** Additional classes added to the tab content body. */
    className?: string;
    /** Indicates if there should be padding around the tab content body */
    hasPadding?: boolean;
}
export declare const TabContentBody: React.FunctionComponent<TabContentBodyProps>;
//# sourceMappingURL=TabContentBody.d.ts.map