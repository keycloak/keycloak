"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Wizard = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const constants_1 = require("../../helpers/constants");
const react_styles_1 = require("@patternfly/react-styles");
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const Modal_1 = require("../Modal");
const WizardFooterInternal_1 = require("./WizardFooterInternal");
const WizardToggle_1 = require("./WizardToggle");
const WizardNav_1 = require("./WizardNav");
const WizardNavItem_1 = require("./WizardNavItem");
const WizardContext_1 = require("./WizardContext");
const WizardHeader_1 = require("./WizardHeader");
class Wizard extends React.Component {
    constructor(props) {
        super(props);
        this.handleKeyClicks = (event) => {
            if (event.keyCode === constants_1.KEY_CODES.ESCAPE_KEY) {
                if (this.state.isNavOpen) {
                    this.setState({ isNavOpen: !this.state.isNavOpen });
                }
                else if (this.props.isOpen) {
                    this.props.onClose();
                }
            }
        };
        this.onNext = () => {
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
            }
            else {
                const newStep = currentStep + 1;
                this.setState({
                    currentStep: newStep
                });
                const { id: prevId, name: prevName } = flattenedSteps[currentStep - 1];
                const { id, name } = flattenedSteps[newStep - 1];
                return onNext && onNext({ id, name }, { prevId, prevName });
            }
        };
        this.onBack = () => {
            const { onBack } = this.props;
            const { currentStep } = this.state;
            const flattenedSteps = this.getFlattenedSteps();
            if (flattenedSteps.length < currentStep) {
                // Previous step was removed, just update the currentStep state
                const adjustedStep = flattenedSteps.length;
                this.setState({
                    currentStep: adjustedStep
                });
            }
            else {
                const newStep = currentStep - 1 <= 0 ? 0 : currentStep - 1;
                this.setState({
                    currentStep: newStep
                });
                const { id: prevId, name: prevName } = flattenedSteps[newStep];
                const { id, name } = flattenedSteps[newStep - 1];
                return onBack && onBack({ id, name }, { prevId, prevName });
            }
        };
        this.goToStep = (step) => {
            const { onGoToStep } = this.props;
            const { currentStep } = this.state;
            const flattenedSteps = this.getFlattenedSteps();
            const maxSteps = flattenedSteps.length;
            if (step < 1) {
                step = 1;
            }
            else if (step > maxSteps) {
                step = maxSteps;
            }
            this.setState({ currentStep: step, isNavOpen: false });
            const { id: prevId, name: prevName } = flattenedSteps[currentStep - 1];
            const { id, name } = flattenedSteps[step - 1];
            return onGoToStep && onGoToStep({ id, name }, { prevId, prevName });
        };
        this.goToStepById = (stepId) => {
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
        this.goToStepByName = (stepName) => {
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
        this.getFlattenedSteps = () => {
            const { steps } = this.props;
            const flattenedSteps = [];
            for (const step of steps) {
                if (step.steps) {
                    for (const childStep of step.steps) {
                        flattenedSteps.push(childStep);
                    }
                }
                else {
                    flattenedSteps.push(step);
                }
            }
            return flattenedSteps;
        };
        this.getFlattenedStepsIndex = (flattenedSteps, stepName) => {
            for (let i = 0; i < flattenedSteps.length; i++) {
                if (flattenedSteps[i].name === stepName) {
                    return i + 1;
                }
            }
            return 0;
        };
        this.initSteps = (steps) => {
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
        this.getElement = (appendTo) => {
            if (typeof appendTo === 'function') {
                return appendTo();
            }
            return appendTo || document.body;
        };
        const newId = Wizard.currentId++;
        this.titleId = props.titleId || `pf-wizard-title-${newId}`;
        this.descriptionId = props.descriptionId || `pf-wizard-description-${newId}`;
        this.state = {
            currentStep: this.props.startAtStep && Number.isInteger(this.props.startAtStep) ? this.props.startAtStep : 1,
            isNavOpen: false
        };
        this.drawerRef = React.createRef();
    }
    componentDidMount() {
        const target = typeof document !== 'undefined' ? document.body : null;
        if (target) {
            target.addEventListener('keydown', this.handleKeyClicks, false);
        }
    }
    componentWillUnmount() {
        const target = (typeof document !== 'undefined' && document.body) || null;
        if (target) {
            target.removeEventListener('keydown', this.handleKeyClicks, false);
        }
    }
    render() {
        const _a = this.props, { 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        width, height, title, description, descriptionComponent, onClose, onSave, onBack, onNext, onGoToStep, className, steps, startAtStep, nextButtonText = 'Next', backButtonText = 'Back', cancelButtonText = 'Cancel', hideClose, closeButtonAriaLabel = 'Close', navAriaLabel, navAriaLabelledBy, mainAriaLabel, mainAriaLabelledBy, hasNoBodyPadding, footer, appendTo, isOpen, titleId, descriptionId, isNavExpandable, hasDrawer, isDrawerExpanded } = _a, rest = tslib_1.__rest(_a, ["width", "height", "title", "description", "descriptionComponent", "onClose", "onSave", "onBack", "onNext", "onGoToStep", "className", "steps", "startAtStep", "nextButtonText", "backButtonText", "cancelButtonText", "hideClose", "closeButtonAriaLabel", "navAriaLabel", "navAriaLabelledBy", "mainAriaLabel", "mainAriaLabelledBy", "hasNoBodyPadding", "footer", "appendTo", "isOpen", "titleId", "descriptionId", "isNavExpandable", "hasDrawer", "isDrawerExpanded"])
        /* eslint-enable @typescript-eslint/no-unused-vars */
        ;
        const { currentStep } = this.state;
        const flattenedSteps = this.getFlattenedSteps();
        const adjustedStep = flattenedSteps.length < currentStep ? flattenedSteps.length : currentStep;
        const activeStep = flattenedSteps[adjustedStep - 1];
        const computedSteps = this.initSteps(steps);
        const firstStep = activeStep === flattenedSteps[0];
        const isValid = activeStep && activeStep.enableNext !== undefined ? activeStep.enableNext : true;
        const nav = (isWizardNavOpen) => {
            const wizNavAProps = {
                isOpen: isWizardNavOpen,
                'aria-label': navAriaLabel,
                'aria-labelledby': (title || navAriaLabelledBy) && (navAriaLabelledBy || this.titleId)
            };
            return (React.createElement(WizardNav_1.WizardNav, Object.assign({}, wizNavAProps), computedSteps.map((step, index) => {
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
                    return (React.createElement(WizardNavItem_1.WizardNavItem, { key: index, id: step.id, content: step.name, isExpandable: isNavExpandable, isCurrent: hasActiveChild, isDisabled: !canJumpToParent, step: navItemStep, onNavItemClick: this.goToStep },
                        React.createElement(WizardNav_1.WizardNav, Object.assign({}, wizNavAProps, { returnList: true }), step.steps.map((childStep, indexChild) => {
                            if (childStep.isFinishedStep) {
                                // Don't show finished step in the side nav
                                return;
                            }
                            navItemStep = this.getFlattenedStepsIndex(flattenedSteps, childStep.name);
                            enabled = childStep.canJumpTo;
                            return (React.createElement(WizardNavItem_1.WizardNavItem, { key: `child_${indexChild}`, id: childStep.id, content: childStep.name, isCurrent: activeStep.name === childStep.name, isDisabled: !enabled, step: navItemStep, onNavItemClick: this.goToStep }));
                        }))));
                }
                navItemStep = this.getFlattenedStepsIndex(flattenedSteps, step.name);
                enabled = step.canJumpTo;
                return (React.createElement(WizardNavItem_1.WizardNavItem, Object.assign({}, step.stepNavItemProps, { key: index, id: step.id, content: step.name, isCurrent: activeStep.name === step.name, isDisabled: !enabled, step: navItemStep, onNavItemClick: this.goToStep })));
            })));
        };
        const context = {
            goToStepById: this.goToStepById,
            goToStepByName: this.goToStepByName,
            onNext: this.onNext,
            onBack: this.onBack,
            onClose,
            activeStep
        };
        const divStyles = Object.assign(Object.assign({}, (height ? { height } : {})), (width ? { width } : {}));
        const wizard = (React.createElement(WizardContext_1.WizardContextProvider, { value: context },
            React.createElement("div", Object.assign({}, rest, { className: react_styles_1.css(wizard_1.default.wizard, activeStep && activeStep.isFinishedStep && 'pf-m-finished', className), style: Object.keys(divStyles).length ? divStyles : undefined }),
                title && (React.createElement(WizardHeader_1.WizardHeader, { titleId: this.titleId, descriptionId: this.descriptionId, onClose: onClose, title: title, description: description, descriptionComponent: descriptionComponent, closeButtonAriaLabel: closeButtonAriaLabel, hideClose: hideClose })),
                React.createElement(WizardToggle_1.WizardToggle, { hasDrawer: hasDrawer, isDrawerExpanded: isDrawerExpanded, mainAriaLabel: mainAriaLabel, isInPage: isOpen === undefined, mainAriaLabelledBy: (title || mainAriaLabelledBy) && (mainAriaLabelledBy || this.titleId), isNavOpen: this.state.isNavOpen, onNavToggle: isNavOpen => this.setState({ isNavOpen }), nav: nav, steps: steps, activeStep: activeStep, hasNoBodyPadding: hasNoBodyPadding }, footer || (React.createElement(WizardFooterInternal_1.WizardFooterInternal, { onNext: this.onNext, onBack: this.onBack, onClose: onClose, isValid: isValid, firstStep: firstStep, activeStep: activeStep, nextButtonText: (activeStep && activeStep.nextButtonText) || nextButtonText, backButtonText: backButtonText, cancelButtonText: cancelButtonText }))))));
        if (isOpen !== undefined) {
            return (React.createElement(Modal_1.Modal, { width: width !== null ? width : undefined, isOpen: isOpen, variant: Modal_1.ModalVariant.large, "aria-labelledby": this.titleId, "aria-describedby": this.descriptionId, showClose: false, hasNoBodyWrapper: true }, wizard));
        }
        return wizard;
    }
}
exports.Wizard = Wizard;
Wizard.displayName = 'Wizard';
Wizard.currentId = 0;
Wizard.defaultProps = {
    title: null,
    description: '',
    descriptionComponent: 'p',
    className: '',
    startAtStep: 1,
    nextButtonText: 'Next',
    backButtonText: 'Back',
    cancelButtonText: 'Cancel',
    hideClose: false,
    closeButtonAriaLabel: 'Close',
    navAriaLabel: null,
    navAriaLabelledBy: null,
    mainAriaLabel: null,
    mainAriaLabelledBy: null,
    hasNoBodyPadding: false,
    onBack: null,
    onNext: null,
    onGoToStep: null,
    width: null,
    height: null,
    footer: null,
    onClose: () => undefined,
    appendTo: null,
    isOpen: undefined,
    isNavExpandable: false,
    hasDrawer: false,
    isDrawerExpanded: false
};
//# sourceMappingURL=Wizard.js.map