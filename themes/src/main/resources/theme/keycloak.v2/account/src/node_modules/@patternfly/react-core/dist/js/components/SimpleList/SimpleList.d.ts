import * as React from 'react';
import { SimpleListItemProps } from './SimpleListItem';
export interface SimpleListProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onSelect'> {
    /** Content rendered inside the SimpleList */
    children?: React.ReactNode;
    /** Additional classes added to the SimpleList container */
    className?: string;
    /** Callback when an item is selected */
    onSelect?: (ref: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>, props: SimpleListItemProps) => void;
    /** Indicates whether component is controlled by its internal state */
    isControlled?: boolean;
}
export interface SimpleListState {
    /** Ref of the current SimpleListItem */
    currentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>;
}
interface SimpleListContextProps {
    currentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>;
    updateCurrentRef: (id: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>, props: SimpleListItemProps) => void;
    isControlled: boolean;
}
export declare const SimpleListContext: React.Context<Partial<SimpleListContextProps>>;
export declare class SimpleList extends React.Component<SimpleListProps, SimpleListState> {
    static displayName: string;
    state: {
        currentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>;
    };
    static defaultProps: SimpleListProps;
    handleCurrentUpdate: (newCurrentRef: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>, itemProps: SimpleListItemProps) => void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=SimpleList.d.ts.map