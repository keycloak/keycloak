import * as React from 'react';
export interface ContextSelectorItemProps {
    /** Anything which can be rendered as Context Selector item */
    children?: React.ReactNode;
    /** Classes applied to root element of the Context Selector item */
    className?: string;
    /** Render Context  Selector item as disabled */
    isDisabled?: boolean;
    /** Callback for click event */
    onClick: (event: React.MouseEvent) => void;
    /** @hide internal index of the item */
    index: number;
    /** Internal callback for ref tracking */
    sendRef: (index: number, current: any) => void;
    /** Link href, indicates item should render as anchor tag */
    href?: string;
}
export declare class ContextSelectorItem extends React.Component<ContextSelectorItemProps> {
    static displayName: string;
    static defaultProps: ContextSelectorItemProps;
    ref: React.RefObject<HTMLButtonElement & HTMLAnchorElement>;
    componentDidMount(): void;
    render(): JSX.Element;
}
//# sourceMappingURL=ContextSelectorItem.d.ts.map