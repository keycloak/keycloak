import * as React from 'react';
export interface DropdownToggleActionProps {
    /** Additional classes added to the DropdownToggleAction */
    className?: string;
    /** Flag to show if the action button is disabled */
    isDisabled?: boolean;
    /** A callback for when the action button is clicked */
    onClick?: (event: React.MouseEvent<HTMLButtonElement>) => void;
    /** Element to be rendered inside the <button> */
    children?: React.ReactNode;
    /** Id of the action button */
    id?: string;
    /** Aria-label of the action button */
    'aria-label'?: string;
}
export declare class DropdownToggleAction extends React.Component<DropdownToggleActionProps> {
    static displayName: string;
    static defaultProps: DropdownToggleActionProps;
    render(): JSX.Element;
}
//# sourceMappingURL=DropdownToggleAction.d.ts.map