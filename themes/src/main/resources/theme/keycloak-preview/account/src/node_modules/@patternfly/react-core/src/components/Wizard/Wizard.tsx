import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { canUseDOM } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/Wizard/wizard';
import { Backdrop } from '../Backdrop';
import { Bullseye } from '../../layouts/Bullseye';
import { WizardHeader } from './WizardHeader';
import { WizardFooterInternal } from './WizardFooterInternal';
import { WizardToggle } from './WizardToggle';
import { WizardNav } from './WizardNav';
import { WizardNavItem } from './WizardNavItem';
import { WizardContextProvider } from './WizardContext';
import { PickOptional } from '../../helpers/typeUtils';
// Can't use ES6 imports :(
// The types for it are also wrong, we should probably ditch this dependency.
import { FocusTrap } from '../../helpers';

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

export type WizardStepFunctionType = (
  newStep: { id?: string | number; name: string },
  prevStep: { prevId?: string | number; prevName: string }
) => void;

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

export class Wizard extends React.Component<WizardProps, WizardState> {
  private static currentId = 0;
  static defaultProps: PickOptional<WizardProps> = {
    isOpen: false,
    isInPage: false,
    isCompactNav: false,
    isFullHeight: false,
    isFullWidth: false,
    title: '',
    description: '',
    className: '',
    startAtStep: 1,
    nextButtonText: 'Next',
    backButtonText: 'Back',
    cancelButtonText: 'Cancel',
    ariaLabelCloseButton: 'Close',
    ariaLabelNav: 'Steps',
    hasBodyPadding: true,
    onBack: null as WizardStepFunctionType,
    onNext: null as WizardStepFunctionType,
    onGoToStep: null as WizardStepFunctionType,
    width: null as string,
    height: null as string,
    footer: null as React.ReactNode,
    onClose: () => undefined as any,
    appendTo: null as HTMLElement
  };
  private container: HTMLDivElement;
  private titleId: string;
  private descriptionId: string;
  private isModal: boolean;

  constructor(props: WizardProps) {
    super(props);
    const newId = Wizard.currentId++;
    this.isModal = !props.isInPage;
    if (this.isModal) {
      this.titleId = `pf-wizard-title-${newId}`;
      this.descriptionId = `pf-wizard-description-${newId}`;
    }

    this.state = {
      currentStep: this.props.startAtStep && Number.isInteger(this.props.startAtStep) ? this.props.startAtStep : 1,
      isNavOpen: false
    };
  }

  private handleKeyClicks = (event: KeyboardEvent): void => {
    if (event.keyCode === KEY_CODES.ESCAPE_KEY) {
      if (this.state.isNavOpen) {
        this.setState({ isNavOpen: !this.state.isNavOpen });
      } else if (this.props.isOpen) {
        this.props.onClose();
      }
    }
  };

  private toggleSiblingsFromScreenReaders = (hide: boolean): void => {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    const bodyChildren = target.children;
    for (const child of Array.from(bodyChildren)) {
      if (child !== this.container) {
        hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
      }
    }
  };

  private onNext = (): void => {
    const { onNext, onClose, onSave } = this.props;
    const { currentStep } = this.state;
    const flattenedSteps = this.getFlattenedSteps();
    const maxSteps = flattenedSteps.length;
    if (currentStep >= maxSteps) {
      // Hit the save button at the end of the wizard
      if (onSave) {
        return onSave();
      }
      return onClose();
    } else {
      const newStep = currentStep + 1;
      this.setState({
        currentStep: newStep
      });
      const { id: prevId, name: prevName } = flattenedSteps[currentStep - 1];
      const { id, name } = flattenedSteps[newStep - 1];
      return onNext && onNext({ id, name }, { prevId, prevName });
    }
  };

  private onBack = (): void => {
    const { onBack } = this.props;
    const { currentStep } = this.state;
    const flattenedSteps = this.getFlattenedSteps();
    if (flattenedSteps.length < currentStep) {
      // Previous step was removed, just update the currentStep state
      const adjustedStep = flattenedSteps.length;
      this.setState({
        currentStep: adjustedStep
      });
    } else {
      const newStep = currentStep - 1 <= 0 ? 0 : currentStep - 1;
      this.setState({
        currentStep: newStep
      });
      const { id: prevId, name: prevName } = flattenedSteps[newStep];
      const { id, name } = flattenedSteps[newStep - 1];
      return onBack && onBack({ id, name }, { prevId, prevName });
    }
  };

  private goToStep = (step: number): void => {
    const { onGoToStep } = this.props;
    const { currentStep } = this.state;
    const flattenedSteps = this.getFlattenedSteps();
    const maxSteps = flattenedSteps.length;
    if (step < 1) {
      step = 1;
    } else if (step > maxSteps) {
      step = maxSteps;
    }
    this.setState({ currentStep: step, isNavOpen: false });
    const { id: prevId, name: prevName } = flattenedSteps[currentStep - 1];
    const { id, name } = flattenedSteps[step - 1];
    return onGoToStep && onGoToStep({ id, name }, { prevId, prevName });
  };

  private goToStepById = (stepId: number | string): void => {
    const flattenedSteps = this.getFlattenedSteps();
    let step;
    for (let i = 0; i < flattenedSteps.length; i++) {
      if (flattenedSteps[i].id === stepId) {
        step = i + 1;
        break;
      }
    }
    if (step) {
      this.setState({ currentStep: step });
    }
  };

  private goToStepByName = (stepName: string): void => {
    const flattenedSteps = this.getFlattenedSteps();
    let step;
    for (let i = 0; i < flattenedSteps.length; i++) {
      if (flattenedSteps[i].name === stepName) {
        step = i + 1;
        break;
      }
    }
    if (step) {
      this.setState({ currentStep: step });
    }
  };

  private getFlattenedSteps = (): WizardStep[] => {
    const { steps } = this.props;
    const flattenedSteps: WizardStep[] = [];
    for (const step of steps) {
      if (step.steps) {
        for (const childStep of step.steps) {
          flattenedSteps.push(childStep);
        }
      } else {
        flattenedSteps.push(step);
      }
    }
    return flattenedSteps;
  };

  private getFlattenedStepsIndex = (flattenedSteps: WizardStep[], stepName: string): number => {
    for (let i = 0; i < flattenedSteps.length; i++) {
      if (flattenedSteps[i].name === stepName) {
        return i + 1;
      }
    }

    return 0;
  };

  private initSteps = (steps: WizardStep[]): WizardStep[] => {
    // Set default Step values
    for (let i = 0; i < steps.length; i++) {
      if (steps[i].steps) {
        for (let j = 0; j < steps[i].steps.length; j++) {
          steps[i].steps[j] = Object.assign({ canJumpTo: true }, steps[i].steps[j]);
        }
      }
      steps[i] = Object.assign({ canJumpTo: true }, steps[i]);
    }
    return steps;
  };

  getElement = (appendTo: HTMLElement | (() => HTMLElement)) => {
    if (typeof appendTo === 'function') {
      return appendTo();
    }
    return appendTo || document.body;
  };

  componentDidMount() {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    if (this.isModal) {
      if (this.container) {
        target.appendChild(this.container);
      }
      this.toggleSiblingsFromScreenReaders(true);
      target.addEventListener('keydown', this.handleKeyClicks, false);
    }
  }

  componentWillUnmount() {
    const { appendTo } = this.props;
    const target: HTMLElement = this.getElement(appendTo);
    if (this.isModal) {
      if (this.container) {
        target.removeChild(this.container);
      }
      this.toggleSiblingsFromScreenReaders(false);
      target.removeEventListener('keydown', this.handleKeyClicks, false);
    }
  }

  render() {
    if (this.isModal) {
      if (!canUseDOM) {
        return null;
      }
      if (!this.container) {
        this.container = document.createElement('div');
      }
    }
    const {
      /* eslint-disable @typescript-eslint/no-unused-vars */
      isOpen,
      isInPage,
      isFullHeight,
      isFullWidth,
      width,
      height,
      title,
      description,
      onClose,
      onSave,
      onBack,
      onNext,
      onGoToStep,
      className,
      steps,
      startAtStep,
      nextButtonText = 'Next',
      backButtonText = 'Back',
      cancelButtonText = 'Cancel',
      ariaLabelCloseButton = 'Close',
      ariaLabelNav,
      hasBodyPadding,
      footer,
      isCompactNav,
      appendTo,
      ...rest
      /* eslint-enable @typescript-eslint/no-unused-vars */
    } = this.props;
    const { currentStep } = this.state;
    const flattenedSteps = this.getFlattenedSteps();
    const adjustedStep = flattenedSteps.length < currentStep ? flattenedSteps.length : currentStep;
    const activeStep = flattenedSteps[adjustedStep - 1];
    const computedSteps: WizardStep[] = this.initSteps(steps);
    const firstStep = activeStep === flattenedSteps[0];
    const isValid = activeStep && activeStep.enableNext !== undefined ? activeStep.enableNext : true;
    const setFullWidth = isFullWidth || width;
    const setFullHeight = isFullHeight || height;

    const nav = (isWizardNavOpen: boolean) => (
      <WizardNav isOpen={isWizardNavOpen} ariaLabel={ariaLabelNav}>
        {computedSteps.map((step, index) => {
          if (step.isFinishedStep) {
            // Don't show finished step in the side nav
            return;
          }
          let enabled;
          let navItemStep;
          if (step.steps) {
            let hasActiveChild = false;
            let canJumpToParent = false;
            for (const subStep of step.steps) {
              if (activeStep.name === subStep.name) {
                // one of the children matches
                hasActiveChild = true;
              }
              if (subStep.canJumpTo) {
                canJumpToParent = true;
              }
            }
            navItemStep = this.getFlattenedStepsIndex(flattenedSteps, step.steps[0].name);
            return (
              <WizardNavItem
                key={index}
                text={step.name}
                isCurrent={hasActiveChild}
                isDisabled={!canJumpToParent}
                step={navItemStep}
                onNavItemClick={this.goToStep}
              >
                <WizardNav returnList>
                  {step.steps.map((childStep: WizardStep, indexChild: number) => {
                    if (childStep.isFinishedStep) {
                      // Don't show finished step in the side nav
                      return;
                    }
                    navItemStep = this.getFlattenedStepsIndex(flattenedSteps, childStep.name);
                    enabled = childStep.canJumpTo;
                    return (
                      <WizardNavItem
                        key={`child_${indexChild}`}
                        text={childStep.name}
                        isCurrent={activeStep.name === childStep.name}
                        isDisabled={!enabled}
                        step={navItemStep}
                        onNavItemClick={this.goToStep}
                      />
                    );
                  })}
                </WizardNav>
              </WizardNavItem>
            );
          }
          navItemStep = this.getFlattenedStepsIndex(flattenedSteps, step.name);
          enabled = step.canJumpTo;
          return (
            <WizardNavItem
              key={index}
              text={step.name}
              isCurrent={activeStep.name === step.name}
              isDisabled={!enabled}
              step={navItemStep}
              onNavItemClick={this.goToStep}
            />
          );
        })}
      </WizardNav>
    );

    const context = {
      goToStepById: this.goToStepById,
      goToStepByName: this.goToStepByName,
      onNext: this.onNext,
      onBack: this.onBack,
      onClose,
      activeStep
    };

    if (this.isModal && !isOpen) {
      return null;
    }

    const wizard = (
      <WizardContextProvider value={context}>
        <div
          {...rest}
          className={css(
            styles.wizard,
            !this.isModal && styles.modifiers.inPage,
            isCompactNav && 'pf-m-compact-nav',
            activeStep.isFinishedStep && 'pf-m-finished',
            setFullWidth && styles.modifiers.fullWidth,
            setFullHeight && styles.modifiers.fullHeight,
            className
          )}
          {...(this.isModal && {
            role: 'dialog',
            'aria-modal': 'true',
            'aria-labelledby': this.titleId,
            'aria-describedby': description ? this.descriptionId : undefined
          })}
        >
          {this.isModal && (
            <WizardHeader
              titleId={this.titleId}
              descriptionId={this.descriptionId}
              onClose={onClose}
              title={title}
              description={description}
              ariaLabelCloseButton={ariaLabelCloseButton}
            />
          )}
          <WizardToggle
            isNavOpen={this.state.isNavOpen}
            onNavToggle={isNavOpen => this.setState({ isNavOpen })}
            nav={nav}
            steps={steps}
            activeStep={activeStep}
            hasBodyPadding={hasBodyPadding}
          >
            {footer || (
              <WizardFooterInternal
                onNext={this.onNext}
                onBack={this.onBack}
                onClose={onClose}
                isValid={isValid}
                firstStep={firstStep}
                activeStep={activeStep}
                nextButtonText={activeStep.nextButtonText || nextButtonText}
                backButtonText={backButtonText}
                cancelButtonText={cancelButtonText}
              />
            )}
          </WizardToggle>
        </div>
      </WizardContextProvider>
    );

    return this.isModal
      ? ReactDOM.createPortal(
          <FocusTrap focusTrapOptions={{ clickOutsideDeactivates: true }}>
            <Backdrop>
              <Bullseye>{wizard}</Bullseye>
            </Backdrop>
          </FocusTrap>,
          this.container
        )
      : wizard;
  }
}
