import * as React from 'react';
export interface PanelHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the panel header */
    children?: React.ReactNode;
    /** Class to add to outer div */
    className?: string;
}
export declare const PanelHeader: React.FunctionComponent<PanelHeaderProps>;
//# sourceMappingURL=PanelHeader.d.ts.map