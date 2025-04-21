import * as React from 'react';
export interface PanelMainProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the panel main div */
    children?: React.ReactNode;
    /** Class to add to outer div */
    className?: string;
    /** Max height of the panel main div as a string with the value and unit */
    maxHeight?: string;
}
export declare const PanelMain: React.FunctionComponent<PanelMainProps>;
//# sourceMappingURL=PanelMain.d.ts.map