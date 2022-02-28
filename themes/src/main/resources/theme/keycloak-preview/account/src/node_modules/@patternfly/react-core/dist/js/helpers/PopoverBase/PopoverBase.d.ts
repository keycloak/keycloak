import * as React from 'react';
import { Instance, Props, Content } from 'tippy.js';
export interface ITippyProps {
    /** Props for tippy */
    [_: string]: any;
}
interface PopoverBaseProps extends ITippyProps {
    children: React.ReactNode;
    content: React.ReactNode;
    isEnabled?: boolean;
    isVisible?: boolean;
    onCreate?: (tippy: Instance<Props>) => void;
    trigger?: string;
}
interface PopoverBaseState {
    isMounted: boolean;
}
declare class PopoverBase extends React.Component<PopoverBaseProps, PopoverBaseState> {
    state: {
        isMounted: boolean;
    };
    container: HTMLDivElement;
    tip: Instance<Props>;
    static defaultProps: {
        trigger: string;
    };
    get isReactElementContent(): boolean;
    get options(): {
        content: Content;
    };
    get isManualTrigger(): boolean;
    componentDidMount(): void;
    componentDidUpdate(): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
export default PopoverBase;
