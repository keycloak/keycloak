import * as React from 'react';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';
export interface ToolbarGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered as one toolbar group */
    children?: React.ReactNode;
    /** Classes applied to toolbar group */
    className?: string;
}
export declare const ToolbarGroup: React.FunctionComponent<ToolbarGroupProps>;
