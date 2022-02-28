import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface WizardStep {
    /** Optional identifier */
    id?: string | number;
    /** The name of the step */
    name: string;
    /** The component to render in the main body */
    component?: any;
    /** Setting to true hides the side nav and footer */
    isFinishedStep?: boolean;
    /** Enables or disables the step in the navigation. Enabled by default. */
    canJumpTo?: boolean;
    /** Sub steps */
    steps?: WizardStep[];
    /** (Unused if footer is controlled) Can change the Next button text. If nextButtonText is also set for the Wizard, this step specific one overrides it. */
    nextButtonText?: string;
    /** (Unused if footer is controlled) The condition needed to enable the Next button */
    enableNext?: boolean;
    /** (Unused if footer is controlled) True to hide the Cancel button */
    hideCancelButton?: boolean;
    /** (Unused if footer is controlled) True to hide the Back button */
    hideBackButton?: boolean;
}
export declare type WizardStepFunctionType = (newStep: {
    id?: string | number;
    name: string;
}, prevStep: {
    prevId?: string | number;
    prevName: string;
}) => void;
export interface WizardProps extends React.HTMLProps<HTMLDivElement> {
    /** True to show the wizard (not applicable for isInPage)*/
    isOpen?: boolean;
    /** True to show the wizard without the modal */
    isInPage?: boolean;
    /** If true makes the navigation more compact */
    isCompactNav?: boolean;
    /** True to set full height wizard */
    isFullHeight?: boolean;
    /** True to set full width wizard */
    isFullWidth?: boolean;
    /** Custom width of the wizard */
    width?: number | string;
    /** Custom height of the wizard */
    height?: number | string;
    /** The wizard title (required unless isInPage is used) */
    title?: string;
    /** The wizard description */
    description?: string;
    /** Callback function to close the wizard */
    onClose?: () => void;
    /** Callback function when a step in the nav is clicked */
    onGoToStep?: WizardStepFunctionType;
    /** Additional classes spread to the Wizard */
    className?: string;
    /** The wizard steps configuration object */
    steps: WizardStep[];
    /** The current step the wizard is on (1 or higher) */
    startAtStep?: number;
    /** aria-label for the Nav */
    ariaLabelNav?: string;
    /** Can remove the default padding around the main body content by setting this to false */
    hasBodyPadding?: boolean;
    /** (Use to control the footer) Passing in a footer component lets you control the buttons yourself */
    footer?: React.ReactNode;
    /** (Unused if footer is controlled) Callback function to save at the end of the wizard, if not specified uses onClose */
    onSave?: () => void;
    /** (Unused if footer is controlled) Callback function after Next button is clicked */
    onNext?: WizardStepFunctionType;
    /** (Unused if footer is controlled) Callback function after Back button is clicked */
    onBack?: WizardStepFunctionType;
    /** (Unused if footer is controlled) The Next button text */
    nextButtonText?: string;
    /** (Unused if footer is controlled) The Back button text */
    backButtonText?: string;
    /** (Unused if footer is controlled) The Cancel button text */
    cancelButtonText?: string;
    /** (Unused if footer is controlled) aria-label for the close button */
    ariaLabelCloseButton?: string;
    /** The parent container to append the modal to. Defaults to document.body */
    appendTo?: HTMLElement | (() => HTMLElement);
}
interface WizardState {
    currentStep: number;
    isNavOpen: boolean;
}
export declare class Wizard extends React.Component<WizardProps, WizardState> {
    private static currentId;
    static defaultProps: PickOptional<WizardProps>;
    private container;
    private titleId;
    private descriptionId;
    private isModal;
    constructor(props: WizardProps);
    private handleKeyClicks;
    private toggleSiblingsFromScreenReaders;
    private onNext;
    private onBack;
    private goToStep;
    private goToStepById;
    private goToStepByName;
    private getFlattenedSteps;
    private getFlattenedStepsIndex;
    private initSteps;
    getElement: (appendTo: HTMLElement | (() => HTMLElement)) => HTMLElement;
    componentDidMount(): void;
    componentWillUnmount(): void;
    render(): JSX.Element;
}
export {};
