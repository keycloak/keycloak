import * as React from 'react';
import { ClipboardCopyProps } from './ClipboardCopy';
import { PickOptional } from '../../helpers/typeUtils';
export interface ClipboardCopyExpandedProps extends Omit<ClipboardCopyProps, 'onChange'> {
    className?: string;
    children: React.ReactNode;
    onChange?: (text: string, e: React.FormEvent<HTMLDivElement>) => void;
    isReadOnly?: boolean;
    isCode?: boolean;
}
export declare class ClipboardCopyExpanded extends React.Component<ClipboardCopyExpandedProps> {
    static displayName: string;
    constructor(props: any);
    static defaultProps: PickOptional<ClipboardCopyExpandedProps>;
    render(): JSX.Element;
}
//# sourceMappingURL=ClipboardCopyExpanded.d.ts.map