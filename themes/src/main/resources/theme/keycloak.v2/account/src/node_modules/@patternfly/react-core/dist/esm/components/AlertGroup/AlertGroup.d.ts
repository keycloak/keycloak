import * as React from 'react';
export interface AlertGroupProps extends Omit<React.HTMLProps<HTMLUListElement>, 'className'> {
    /** Additional classes added to the AlertGroup */
    className?: string;
    /** Alerts to be rendered in the AlertGroup */
    children?: React.ReactNode;
    /** Toast notifications are positioned at the top right corner of the viewport */
    isToast?: boolean;
    /** Turns the container into a live region so that changes to content within the AlertGroup, such as appending an Alert, are reliably announced to assistive technology. */
    isLiveRegion?: boolean;
    /** Determine where the alert is appended to */
    appendTo?: HTMLElement | (() => HTMLElement);
    /** Function to call if user clicks on overflow message */
    onOverflowClick?: () => void;
    /** Custom text to show for the overflow message */
    overflowMessage?: string;
}
interface AlertGroupState {
    container: HTMLElement;
}
export declare class AlertGroup extends React.Component<AlertGroupProps, AlertGroupState> {
    static displayName: string;
    state: AlertGroupState;
    componentDidMount(): void;
    componentWillUnmount(): void;
    getTargetElement(): HTMLElement;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=AlertGroup.d.ts.map