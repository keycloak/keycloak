import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface SkipToContentProps extends React.HTMLProps<HTMLAnchorElement> {
    /** The skip to content link. */
    href: string;
    /** Content to display within the skip to content component, typically a string. */
    children?: React.ReactNode;
    /** Additional styles to apply to the skip to content component. */
    className?: string;
    /** Forces the skip to content to display. This is primarily for demonstration purposes and would not normally be used. */
    show?: boolean;
}
export declare class SkipToContent extends React.Component<SkipToContentProps> {
    static displayName: string;
    static defaultProps: PickOptional<SkipToContentProps>;
    componentRef: React.RefObject<HTMLAnchorElement>;
    componentDidMount(): void;
    render(): JSX.Element;
}
//# sourceMappingURL=SkipToContent.d.ts.map