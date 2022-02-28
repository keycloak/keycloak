import * as React from 'react';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';
export interface ToolbarSectionProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered as toolbar section */
    children?: React.ReactNode;
    /** Classes applied to toolbar section */
    className?: string;
    /** Aria label applied to toolbar section */
    'aria-label'?: string;
}
export declare const ToolbarSection: React.FunctionComponent<ToolbarSectionProps>;
