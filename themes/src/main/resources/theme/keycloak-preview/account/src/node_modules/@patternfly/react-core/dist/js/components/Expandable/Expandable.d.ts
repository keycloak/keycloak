import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface ExpandableProps {
    /** Content rendered inside the Expandable Component */
    children: React.ReactNode;
    /** Additional classes added to the Expandable Component */
    className?: string;
    /** Flag to indicate if the content is expanded */
    isExpanded?: boolean;
    /** Text that appears in the toggle */
    toggleText?: string;
    /** Text that appears in the toggle when expanded (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
    toggleTextExpanded?: string;
    /** Text that appears in the toggle when collapsed (will override toggleText if both are specified; used for uncontrolled expandable with dynamic toggle text) */
    toggleTextCollapsed?: string;
    /** Callback function to toggle the expandable content */
    onToggle?: () => void;
    /** Forces focus state */
    isFocused?: boolean;
    /** Forces hover state */
    isHovered?: boolean;
    /** Forces active state */
    isActive?: boolean;
}
interface ExpandableState {
    isExpanded: boolean;
}
export declare class Expandable extends React.Component<ExpandableProps, ExpandableState> {
    constructor(props: ExpandableProps);
    static defaultProps: PickOptional<ExpandableProps>;
    private calculateToggleText;
    render(): JSX.Element;
}
export {};
