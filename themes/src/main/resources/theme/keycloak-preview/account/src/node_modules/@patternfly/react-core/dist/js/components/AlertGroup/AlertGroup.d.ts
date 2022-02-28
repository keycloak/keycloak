import * as React from 'react';
export interface AlertGroupProps extends Omit<React.HTMLProps<HTMLUListElement>, 'className'> {
    /** Additional classes added to the AlertGroup */
    className?: string;
    /** Alerts to be rendered in the AlertGroup */
    children?: React.ReactNode;
    /** Toast notifications are positioned at the top right corner of the viewport */
    isToast?: boolean;
    /** Determine where the alert is appended to */
    appendTo?: HTMLElement | (() => HTMLElement);
}
interface AlertGroupState {
    container: HTMLElement;
}
export declare class AlertGroup extends React.Component<AlertGroupProps, AlertGroupState> {
    state: AlertGroupState;
    componentDidMount(): void;
    componentWillUnmount(): void;
    getTargetElement(): HTMLElement;
    render(): JSX.Element;
}
export {};
