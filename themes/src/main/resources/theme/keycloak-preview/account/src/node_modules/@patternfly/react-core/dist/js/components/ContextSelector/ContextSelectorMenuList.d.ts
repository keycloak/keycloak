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
    static defaultProps: ContextSelectorMenuListProps;
    refsCollection: any;
    sendRef: (index: number, ref: any) => void;
    extendChildren(): React.ReactElement<any, string | ((props: any) => React.ReactElement<any, string | any | (new (props: any) => React.Component<any, any, any>)>) | (new (props: any) => React.Component<any, any, any>)>[];
    render: () => JSX.Element;
}
