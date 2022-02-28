import * as React from 'react';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';
export interface ToolbarProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered as toolbar content */
    children?: React.ReactNode;
    /** Classes applied to toolbar parent */
    className?: string;
}
export declare const Toolbar: React.FunctionComponent<ToolbarProps>;
