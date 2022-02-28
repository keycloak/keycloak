import * as React from 'react';
import { Options as FocusTrapOptions, FocusTrap as IFocusTrap } from 'focus-trap';
interface FocusTrapProps {
    children: React.ReactNode;
    className?: string;
    active?: boolean;
    paused?: boolean;
    focusTrapOptions?: FocusTrapOptions;
}
export declare class FocusTrap extends React.Component<FocusTrapProps> {
    previouslyFocusedElement: HTMLElement;
    focusTrap: IFocusTrap;
    divRef: React.RefObject<HTMLDivElement>;
    static defaultProps: {
        active: boolean;
        paused: boolean;
        focusTrapOptions: {};
    };
    constructor(props: FocusTrapProps);
    componentDidMount(): void;
    componentDidUpdate(prevProps: FocusTrapProps): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
export {};
