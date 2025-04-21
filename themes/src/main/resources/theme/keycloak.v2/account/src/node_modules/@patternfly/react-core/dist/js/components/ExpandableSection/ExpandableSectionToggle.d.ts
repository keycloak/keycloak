import * as React from 'react';
export interface ExpandableSectionToggleProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the expandable toggle. */
    children?: React.ReactNode;
    /** Additional classes added to the expandable toggle. */
    className?: string;
    /** Flag indicating if the expandable section is expanded. */
    isExpanded?: boolean;
    /** Callback function to toggle the expandable content. */
    onToggle?: (isExpanded: boolean) => void;
    /** ID of the toggle's respective expandable section content. */
    contentId?: string;
    /** Direction the toggle arrow should point when the expandable section is expanded. */
    direction?: 'up' | 'down';
}
export declare const ExpandableSectionToggle: React.FunctionComponent<ExpandableSectionToggleProps>;
//# sourceMappingURL=ExpandableSectionToggle.d.ts.map