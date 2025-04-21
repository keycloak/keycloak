import * as React from 'react';
import { Options as FocusTrapOptions, FocusTrap as IFocusTrap } from 'focus-trap';
interface FocusTrapProps extends React.HTMLProps<HTMLDivElement> {
    children: React.ReactNode;
    className?: string;
    active?: boolean;
    paused?: boolean;
    focusTrapOptions?: FocusTrapOptions;
    /** Prevent from scrolling to the previously focused element on deactivation */
    preventScrollOnDeactivate?: boolean;
}
export declare class FocusTrap extends React.Component<FocusTrapProps> {
    static displayName: string;
    previouslyFocusedElement: HTMLElement;
    focusTrap: IFocusTrap;
    divRef: React.RefObject<HTMLDivElement>;
    static defaultProps: {
        active: boolean;
        paused: boolean;
        focusTrapOptions: {};
        preventScrollOnDeactivate: boolean;
    };
    constructor(props: FocusTrapProps);
    componentDidMount(): void;
    componentDidUpdate(prevProps: FocusTrapProps): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=FocusTrap.d.ts.map