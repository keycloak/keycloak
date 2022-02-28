import * as React from 'react';
import '@patternfly/react-styles/css/layouts/Toolbar/toolbar.css';
export interface ToolbarItemProps extends React.HTMLProps<HTMLDivElement> {
    /** Anything that can be rendered as toolbar item content */
    children?: React.ReactNode;
    /** Classes applied to toolbar item */
    className?: string;
}
export declare const ToolbarItem: React.FunctionComponent<ToolbarItemProps>;
