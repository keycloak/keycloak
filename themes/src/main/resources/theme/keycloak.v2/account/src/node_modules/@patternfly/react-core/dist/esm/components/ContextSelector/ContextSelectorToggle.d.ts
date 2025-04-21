import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface ContextSelectorToggleProps {
    /** HTML ID of toggle */
    id: string;
    /** Classes applied to root element of toggle */
    className?: string;
    /** Text that appears in the Context Selector Toggle */
    toggleText?: string;
    /** Flag to indicate if menu is opened */
    isOpen?: boolean;
    /** Callback called when toggle is clicked */
    onToggle?: (event: any, value: boolean) => void;
    /** Callback for toggle open on keyboard entry */
    onEnter?: () => void;
    /** Element which wraps toggle */
    parentRef?: any;
    /** Forces active state */
    isActive?: boolean;
    /** Flag to indicate the toggle has no border or background */
    isPlain?: boolean;
    /** Flag to indicate if toggle is textual toggle */
    isText?: boolean;
}
export declare class ContextSelectorToggle extends React.Component<ContextSelectorToggleProps> {
    static displayName: string;
    static defaultProps: PickOptional<ContextSelectorToggleProps>;
    toggle: React.RefObject<HTMLButtonElement>;
    componentDidMount: () => void;
    componentWillUnmount: () => void;
    onDocClick: (event: any) => void;
    onEscPress: (event: any) => void;
    onKeyDown: (event: any) => void;
    render(): JSX.Element;
}
//# sourceMappingURL=ContextSelectorToggle.d.ts.map