import * as React from 'react';
export interface ContextSelectorMenuListProps {
    /** Content rendered inside the Context Selector Menu */
    children?: React.ReactNode;
    /** Classess applied to root element of Context Selector menu */
    className?: string;
    /** Flag to indicate if Context Selector menu is opened */
    isOpen?: boolean;
}
export declare class ContextSelectorMenuList extends React.Component<ContextSelectorMenuListProps> {
    static displayName: string;
    static defaultProps: ContextSelectorMenuListProps;
    refsCollection: any;
    sendRef: (index: number, ref: any) => void;
    extendChildren(): React.ReactElement<any, string | React.JSXElementConstructor<any>>[];
    render: () => JSX.Element;
}
//# sourceMappingURL=ContextSelectorMenuList.d.ts.map