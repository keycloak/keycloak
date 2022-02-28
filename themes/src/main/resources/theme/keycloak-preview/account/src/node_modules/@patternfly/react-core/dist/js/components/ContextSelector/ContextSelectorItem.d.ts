import * as React from 'react';
export interface ContextSelectorItemProps {
    /** Anything which can be rendered as Context Selector item */
    children?: React.ReactNode;
    /** Classes applied to root element of the Context Selector item */
    className?: string;
    /** Render Context  Selector item as disabled */
    isDisabled?: boolean;
    /** Forces display of the hover state of the element */
    isHovered?: boolean;
    /** Callback for click event */
    onClick: (event: React.MouseEvent) => void;
    /** internal index of the item */
    index: number;
    /** Internal callback for ref tracking */
    sendRef: (index: number, current: any) => void;
}
export declare class ContextSelectorItem extends React.Component<ContextSelectorItemProps> {
    static defaultProps: ContextSelectorItemProps;
    ref: React.RefObject<HTMLButtonElement>;
    componentDidMount(): void;
    render(): JSX.Element;
}
