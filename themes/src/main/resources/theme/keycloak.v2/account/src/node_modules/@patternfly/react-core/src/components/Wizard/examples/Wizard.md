---
id: Wizard
section: components
cssPrefix: pf-c-wizard
propComponents:
  ['Wizard', 'WizardNav', 'WizardNavItem', 'WizardHeader', 'WizardBody', 'WizardFooter', 'WizardToggle', 'WizardStep']
---

import { Button, Drawer, DrawerActions, DrawerCloseButton, DrawerColorVariant,
DrawerContent, DrawerContentBody, DrawerHead, DrawerPanelContent, DrawerSection, Wizard, WizardFooter, WizardContextConsumer, ModalVariant, Alert, EmptyState, EmptyStateIcon, EmptyStateBody, EmptyStateSecondaryActions, Title, Progress } from '@patternfly/react-core';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import SlackHashIcon from '@patternfly/react-icons/dist/esm/icons/slack-hash-icon';
import FinishedStep from './FinishedStep';
import SampleForm from './SampleForm';

## Examples

### Basic

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';

class SimpleWizard extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const steps = [
      { name: 'First step', component: <p>Step 1 content</p> },
      { name: 'Second step', component: <p>Step 2 content</p> },
      { name: 'Third step', component: <p>Step 3 content</p> },
      { name: 'Fourth step', component: <p>Step 4 content</p> },
      { name: 'Review', component: <p>Review step content</p>, nextButtonText: 'Finish' }
    ];
    const title = 'Basic wizard';
    return <Wizard navAriaLabel={`${title} steps`} mainAriaLabel={`${title} content`} steps={steps} height={400} />;
  }
}
```

### Anchors for nav items

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import SlackHashIcon from '@patternfly/react-icons/dist/esm/icons/slack-hash-icon';

class WizardWithNavAnchors extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const steps = [
      {
        name: (
          <div>
            <ExternalLinkAltIcon /> PF3
          </div>
        ),
        component: <p>Step 1: Read about PF3</p>,
        stepNavItemProps: { navItemComponent: 'a', href: 'https://www.patternfly.org/v3/', target: '_blank' }
      },
      {
        name: (
          <div>
            <ExternalLinkAltIcon /> PF4
          </div>
        ),
        component: <p>Step 2: Read about PF4</p>,
        stepNavItemProps: { navItemComponent: 'a', href: 'https://www.patternfly.org/v4/', target: '_blank' }
      },
      {
        name: (
          <div>
            <SlackHashIcon /> Join us on slack
          </div>
        ),
        component: (
          <Button variant="link" component="a" target="_blank" href="https://patternfly.slack.com/">
            Join the conversation
          </Button>
        ),
        stepNavItemProps: { navItemComponent: 'a', href: 'https://patternfly.slack.com/', target: '_blank' }
      }
    ];
    const title = 'Anchor link wizard';
    return <Wizard navAriaLabel={`${title} steps`} mainAriaLabel={`${title} content`} steps={steps} height={400} />;
  }
}
```

### Incrementally enabled steps

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';

class IncrementallyEnabledStepsWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      stepIdReached: 1
    };
    this.onNext = ({ id }) => {
      this.setState({
        stepIdReached: this.state.stepIdReached < id ? id : this.state.stepIdReached
      });
    };
    this.closeWizard = () => {
      console.log('close wizard');
    };
  }

  render() {
    const { stepIdReached } = this.state;

    const steps = [
      { id: 1, name: 'First step', component: <p>Step 1 content</p> },
      { id: 2, name: 'Second step', component: <p>Step 2 content</p>, canJumpTo: stepIdReached >= 2 },
      { id: 3, name: 'Third step', component: <p>Step 3 content</p>, canJumpTo: stepIdReached >= 3 },
      { id: 4, name: 'Fourth step', component: <p>Step 4 content</p>, canJumpTo: stepIdReached >= 4 },
      {
        id: 5,
        name: 'Review',
        component: <p>Review step content</p>,
        nextButtonText: 'Finish',
        canJumpTo: stepIdReached >= 5
      }
    ];
    const title = 'Incrementally enabled wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        onClose={this.closeWizard}
        steps={steps}
        onNext={this.onNext}
        height={400}
      />
    );
  }
}
```

### Expandable steps

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';

class SimpleWizard extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    const steps = [
      {
        name: 'First step',
        steps: [
          { name: 'Substep A', component: <p>Substep A content</p> },
          { name: 'Substep B', component: <p>Substep B content</p> }
        ]
      },
      { name: 'Second step', component: <p>Step 2 content</p> },
      {
        name: 'Third step',
        steps: [
          { name: 'Substep C', component: <p>Substep C content</p> },
          { name: 'Substep D', component: <p>Substep D content</p> }
        ]
      },
      { name: 'Fourth step', component: <p>Step 4 content</p> },
      { name: 'Review', component: <p>Review step content</p>, nextButtonText: 'Finish' }
    ];
    const title = 'Basic wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        steps={steps}
        height={400}
        isNavExpandable
      />
    );
  }
}
```

### Finished

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';
import FinishedStep from './examples/FinishedStep';

class FinishedStepWizard extends React.Component {
  constructor(props) {
    super(props);

    this.closeWizard = () => {
      console.log('close wizard');
    };
  }

  render() {
    const steps = [
      { name: 'First step', component: <p>Step 1 content</p> },
      { name: 'Second step', component: <p>Step 2 content</p> },
      { name: 'Third step', component: <p>Step 3 content</p> },
      { name: 'Fourth step', component: <p>Step 4 content</p> },
      { name: 'Review', component: <p>Review step content</p>, nextButtonText: 'Finish' },
      { name: 'Finish', component: <FinishedStep onClose={this.closeWizard} />, isFinishedStep: true }
    ];
    const title = 'Finished wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        onClose={this.closeWizard}
        steps={steps}
        height={400}
      />
    );
  }
}
```

### Enabled on form validation

```js
import React from 'react';
import { Button, Wizard, Form, FormGroup, TextInput } from '@patternfly/react-core';
import SampleForm from './examples/SampleForm';

class ValidationWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isFormValid: false,
      formValue: 'Thirty',
      allStepsValid: false,
      stepIdReached: 1
    };

    this.closeWizard = () => {
      console.log('close wizard');
    };

    this.onFormChange = (isValid, value) => {
      this.setState(
        {
          isFormValid: isValid,
          formValue: value
        },
        this.areAllStepsValid
      );
    };

    this.areAllStepsValid = () => {
      this.setState({
        allStepsValid: this.state.isFormValid
      });
    };

    this.onNext = ({ id, name }, { prevId, prevName }) => {
      console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
      this.setState({
        stepIdReached: this.state.stepIdReached < id ? id : this.state.stepIdReached
      });
      this.areAllStepsValid();
    };

    this.onBack = ({ id, name }, { prevId, prevName }) => {
      console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
      this.areAllStepsValid();
    };

    this.onGoToStep = ({ id, name }, { prevId, prevName }) => {
      console.log(`current id: ${id}, current name: ${name}, previous id: ${prevId}, previous name: ${prevName}`);
    };

    this.onSave = () => {
      console.log('Saved and closed the wizard');
      this.setState({
        isOpen: false
      });
    };
  }

  render() {
    const { isFormValid, formValue, allStepsValid, stepIdReached } = this.state;

    const steps = [
      { id: 1, name: 'Information', component: <p>Step 1 content</p> },
      {
        name: 'Configuration',
        steps: [
          {
            id: 2,
            name: 'Substep A with validation',
            component: <SampleForm formValue={formValue} isFormValid={isFormValid} onChange={this.onFormChange} />,
            enableNext: isFormValid,
            canJumpTo: stepIdReached >= 2
          },
          { id: 3, name: 'Substep B', component: <p>Substep B</p>, canJumpTo: stepIdReached >= 3 }
        ]
      },
      {
        id: 4,
        name: 'Additional',
        component: <p>Step 3 content</p>,
        enableNext: allStepsValid,
        canJumpTo: stepIdReached >= 4
      },
      {
        id: 5,
        name: 'Review',
        component: <p>Step 4 content</p>,
        nextButtonText: 'Close',
        canJumpTo: stepIdReached >= 5
      }
    ];
    const title = 'Enabled on form validation wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        onClose={this.closeWizard}
        onSave={this.onSave}
        steps={steps}
        onNext={this.onNext}
        onBack={this.onBack}
        onGoToStep={this.onGoToStep}
        height={400}
      />
    );
  }
}
```

### Validate on button press

This example demonstrates how to use the `WizardContextConsumer` to consume the `WizardContext`. `WizardContext` can be used to imperatively move to a specific wizard step.

The definition of the `WizardContext` is as follows:

```
interface WizardContext {
  goToStepById: (stepId: number | string) => void;
  goToStepByName: (stepName: string) => void;
  onNext: () => void;
  onBack: () => void;
  onClose: () => void;
  activeStep: WizardStep;
}
```

```js
import React from 'react';
import { Button, Wizard, WizardFooter, WizardContextConsumer, Alert } from '@patternfly/react-core';
import SampleForm from './examples/SampleForm';
import FinishedStep from './examples/FinishedStep';

class ValidateButtonPressWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      stepsValid: 0
    };

    this.closeWizard = () => {
      console.log('close wizard');
    };

    this.validateLastStep = onNext => {
      const { stepsValid } = this.state;
      if (stepsValid !== 1) {
        this.setState({
          stepsValid: 1
        });
      } else {
        onNext();
      }
    };
  }

  render() {
    const { stepsValid } = this.state;

    const steps = [
      { name: 'First step', component: <p>Step 1 content</p> },
      { name: 'Second step', component: <p>Step 2 content</p> },
      {
        name: 'Final Step',
        component: (
          <>
            {stepsValid === 1 && (
              <div style={{ padding: '15px 0' }}>
                <Alert variant="warning" title="Validation failed, please try again" />
              </div>
            )}
            <SampleForm formValue="Validating on button press" isFormValid={stepsValid !== 1} />
          </>
        )
      },
      { name: 'Finish', component: <FinishedStep onClose={this.closeWizard} />, isFinishedStep: true }
    ];

    const CustomFooter = (
      <WizardFooter>
        <WizardContextConsumer>
          {({ activeStep, goToStepByName, goToStepById, onNext, onBack, onClose }) => {
            if (activeStep.name !== 'Final Step') {
              return (
                <>
                  <Button variant="primary" type="submit" onClick={onNext}>
                    Forward
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={onBack}
                    className={activeStep.name === 'Step 1' ? 'pf-m-disabled' : ''}
                  >
                    Backward
                  </Button>
                  <Button variant="link" onClick={onClose}>
                    Cancel
                  </Button>
                </>
              );
            }
            // Final step buttons
            return (
              <>
                <Button onClick={() => this.validateLastStep(onNext)}>Validate</Button>
                <Button onClick={() => goToStepByName('Step 1')}>Go to Beginning</Button>
              </>
            );
          }}
        </WizardContextConsumer>
      </WizardFooter>
    );
    const title = 'Validate on button press wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        onClose={this.closeWizard}
        footer={CustomFooter}
        steps={steps}
        height={400}
      />
    );
  }
}
```

### Progressive steps

```js
import React from 'react';
import { Button, Radio, Wizard, WizardFooter, WizardContextConsumer, Alert } from '@patternfly/react-core';
import SampleForm from './examples/SampleForm';
import FinishedStep from './examples/FinishedStep';

class ProgressiveWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showCreateStep: false,
      showUpdateStep: false,
      showOptionsStep: false,
      showReviewStep: false,
      getStartedStepRadio: 'Create',
      createStepRadio: 'Quick',
      updateStepRadio: 'Quick'
    };
    this.closeWizard = () => {
      console.log('close wizard');
    };
    this.onGoToStep = ({ id, name }, { prevId, prevName }) => {
      // Remove steps after the currently clicked step
      if (name === 'Get started') {
        this.setState({
          showReviewStep: false,
          showOptionsStep: false,
          showCreateStep: false,
          showUpdateStep: false
        });
      } else if (name === 'Create options' || name === 'Update options') {
        this.setState({
          showReviewStep: false,
          showOptionsStep: false
        });
      } else if (name.indexOf('Substep') > -1) {
        this.setState({
          showReviewStep: false
        });
      }
    };
    this.getNextStep = (activeStep, callback) => {
      if (activeStep.name === 'Get started') {
        if (this.state.getStartedStepRadio === 'Create') {
          this.setState(
            {
              showCreateStep: true,
              showUpdateStep: false,
              showOptionsStep: false,
              showReviewStep: false
            },
            () => {
              callback();
            }
          );
        } else {
          this.setState(
            {
              showCreateStep: false,
              showUpdateStep: true,
              showOptionsStep: false,
              showReviewStep: false
            },
            () => {
              callback();
            }
          );
        }
      } else if (activeStep.name === 'Create options' || activeStep.name === 'Update options') {
        this.setState(
          {
            showOptionsStep: true,
            showReviewStep: false
          },
          () => {
            callback();
          }
        );
      } else if (activeStep.name === 'Substep 3') {
        this.setState(
          {
            showReviewStep: true
          },
          () => {
            callback();
          }
        );
      } else {
        callback();
      }
    };
    this.getPreviousStep = (activeStep, callback) => {
      if (activeStep.name === 'Review') {
        this.setState(
          {
            showReviewStep: false
          },
          () => {
            callback();
          }
        );
      } else if (activeStep.name === 'Substep 1') {
        this.setState(
          {
            showOptionsStep: false
          },
          () => {
            callback();
          }
        );
      } else if (activeStep.name === 'Create options') {
        this.setState(
          {
            showCreateStep: false
          },
          () => {
            callback();
          }
        );
      } else if (activeStep.name === 'Update options') {
        this.setState(
          {
            showUpdateStep: false
          },
          () => {
            callback();
          }
        );
      } else {
        callback();
      }
    };
  }

  render() {
    const {
      stepsValid,
      getStartedStepRadio,
      createStepRadio,
      updateStepRadio,
      showCreateStep,
      showUpdateStep,
      showOptionsStep,
      showReviewStep
    } = this.state;

    const getStartedStep = {
      name: 'Get started',
      component: (
        <div>
          <Radio
            value="Create"
            isChecked={getStartedStepRadio === 'Create'}
            onChange={(_, event) => this.setState({ getStartedStepRadio: event.currentTarget.value })}
            label="Create a new thing"
            name="radio-step-start"
            id="radio-step-start-1"
          />{' '}
          <Radio
            value="Update"
            isChecked={getStartedStepRadio === 'Update'}
            onChange={(_, event) => this.setState({ getStartedStepRadio: event.currentTarget.value })}
            label="Update an existing thing"
            name="radio-step-start"
            id="radio-step-start-2"
          />
        </div>
      )
    };

    const createStep = {
      name: 'Create options',
      component: (
        <div>
          <Radio
            value="Quick"
            isChecked={createStepRadio === 'Quick'}
            onChange={(_, event) => this.setState({ createStepRadio: event.currentTarget.value })}
            label="Quick create"
            name="radio-step-create"
            id="radio-step-create-1"
          />{' '}
          <Radio
            value="Custom"
            isChecked={createStepRadio === 'Custom'}
            onChange={(_, event) => this.setState({ createStepRadio: event.currentTarget.value })}
            label="Custom create"
            name="radio-step-create"
            id="radio-step-create-2"
          />
        </div>
      )
    };

    const updateStep = {
      name: 'Update options',
      component: (
        <div>
          <Radio
            value="Quick"
            isChecked={updateStepRadio === 'Quick'}
            onChange={(_, event) => this.setState({ updateStepRadio: event.currentTarget.value })}
            label="Quick update"
            name="radio-step-update"
            id="radio-step-update-1"
          />{' '}
          <Radio
            value="Custom"
            isChecked={updateStepRadio === 'Custom'}
            onChange={(_, event) => this.setState({ updateStepRadio: event.currentTarget.value })}
            label="Custom update"
            name="radio-step-update"
            id="radio-step-update-2"
          />
        </div>
      )
    };

    const optionsStep = {
      name: showCreateStep ? `${createStepRadio} Options` : `${updateStepRadio} Options`,
      steps: [
        {
          name: 'Substep 1',
          component: 'Substep 1'
        },
        {
          name: 'Substep 2',
          component: 'Substep 2'
        },
        {
          name: 'Substep 3',
          component: 'Substep 3'
        }
      ]
    };

    const reviewStep = {
      name: 'Review',
      component: (
        <div>
          <div>First choice: {getStartedStepRadio}</div>
          <div>Second choice: {showCreateStep ? createStepRadio : updateStepRadio}</div>
        </div>
      )
    };

    const steps = [
      getStartedStep,
      ...(showCreateStep ? [createStep] : []),
      ...(showUpdateStep ? [updateStep] : []),
      ...(showOptionsStep ? [optionsStep] : []),
      ...(showReviewStep ? [reviewStep] : [])
    ];

    const CustomFooter = (
      <WizardFooter>
        <WizardContextConsumer>
          {({ activeStep, goToStepByName, goToStepById, onNext, onBack, onClose }) => {
            return (
              <>
                <Button variant="primary" type="submit" onClick={() => this.getNextStep(activeStep, onNext)}>
                  {activeStep.name === 'Review' ? 'Finish' : 'Next'}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => this.getPreviousStep(activeStep, onBack)}
                  className={activeStep.name === 'Get Started' ? 'pf-m-disabled' : ''}
                >
                  Back
                </Button>
                <Button variant="link" onClick={onClose}>
                  Cancel
                </Button>
              </>
            );
          }}
        </WizardContextConsumer>
      </WizardFooter>
    );
    const title = 'Progressive wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        onClose={this.closeWizard}
        footer={CustomFooter}
        onGoToStep={this.onGoToStep}
        steps={steps}
        height={400}
      />
    );
  }
}
```

### Remember last step

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';

class RememberLastStepWizard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      step: 1
    };
    this.closeWizard = () => {
      console.log('close wizard');
    };
    this.onMove = (curr, prev) => {
      this.setState({
        step: curr.id
      });
    };
    this.onSave = () => {
      this.setState({
        step: 1
      });
    };
  }

  render() {
    const { step } = this.state;

    const steps = [
      { id: 1, name: 'First step', component: <p>Step 1 content</p> },
      { id: 2, name: 'Second step', component: <p>Step 2 content</p> },
      { id: 3, name: 'Third step', component: <p>Step 3 content</p> },
      { id: 4, name: 'Fourth step', component: <p>Step 4 content</p> },
      { id: 5, name: 'Review', component: <p>Review step content</p>, nextButtonText: 'Finish' }
    ];
    const title = 'Remember last step wizard';
    return (
      <Wizard
        navAriaLabel={`${title} steps`}
        mainAriaLabel={`${title} content`}
        startAtStep={step}
        onNext={this.onMove}
        onBack={this.onMove}
        onSave={this.onSave}
        onClose={this.closeWizard}
        description="Simple Wizard Description"
        steps={steps}
        height={400}
      />
    );
  }
}
```

### Wizard in modal

```js
import React from 'react';
import { Button, Wizard } from '@patternfly/react-core';

class WizardInModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false
    };
    this.handleModalToggle = () => {
      this.setState(({ isOpen }) => ({
        isOpen: !isOpen
      }));
    };
  }

  render() {
    const { isOpen } = this.state;

    const steps = [
      { name: 'First step', component: <p>Step 1 content</p> },
      { name: 'Second step', component: <p>Step 2 content</p> },
      { name: 'Third step', component: <p>Step 3 content</p> },
      { name: 'Fourth step', component: <p>Step 4 content</p> },
      { name: 'Review', component: <p>Review step content</p>, nextButtonText: 'Finish' }
    ];
    const title = 'Wizard in modal';
    return (
      <React.Fragment>
        <Button variant="primary" onClick={this.handleModalToggle}>
          Show Modal
        </Button>
        <Wizard
          title={title}
          description="Simple Wizard Description"
          descriptionComponent="div"
          steps={steps}
          onClose={this.handleModalToggle}
          isOpen={isOpen}
        />
      </React.Fragment>
    );
  }
}
```

### Wizard with drawer

```js isBeta
import React from 'react';
import {
  Button,
  DrawerActions,
  DrawerCloseButton,
  DrawerHead,
  DrawerPanelContent,
  Text,
  TextContent,
  Wizard
} from '@patternfly/react-core';

class WizardWithDrawer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isOpen: false,
      isDrawerExpanded: false,
      sectionGray: false,
      panelGray: true,
      contentGray: false
    };

    this.drawerRef = React.createRef();

    this.onExpand = () => {
      this.drawerRef.current && this.drawerRef.current.focus();
    };

    this.onOpenClick = () => {
      this.setState({
        isDrawerExpanded: true
      });
    };

    this.onCloseClick = () => {
      this.setState({
        isDrawerExpanded: false
      });
    };
  }

  render() {
    const { isDrawerExpanded } = this.state;

    const panel1Content = (
      <DrawerPanelContent widths={{ default: 'width_33' }} colorVariant={DrawerColorVariant.light200}>
        <DrawerHead>
          <span tabIndex={isDrawerExpanded ? 0 : -1} ref={this.drawerRef}>
            drawer-panel-1 content
          </span>
          <DrawerActions>
            <DrawerCloseButton onClick={this.onCloseClick} />
          </DrawerActions>
        </DrawerHead>
      </DrawerPanelContent>
    );

    const panel2Content = (
      <DrawerPanelContent widths={{ default: 'width_33' }} colorVariant={DrawerColorVariant.light200}>
        <DrawerHead>
          <span tabIndex={0} ref={this.drawerRef}>
            drawer-panel-2 content
          </span>
          <DrawerActions>
            <DrawerCloseButton onClick={this.onCloseClick} />
          </DrawerActions>
        </DrawerHead>
      </DrawerPanelContent>
    );

    const panel3Content = (
      <DrawerPanelContent widths={{ default: 'width_33' }} colorVariant={DrawerColorVariant.light200}>
        <DrawerHead>
          <span tabIndex={0} ref={this.drawerRef}>
            drawer-panel-3 content
          </span>
          <DrawerActions>
            <DrawerCloseButton onClick={this.onCloseClick} />
          </DrawerActions>
        </DrawerHead>
      </DrawerPanelContent>
    );

    const drawerToggleButton = (
      <Button isInline variant="link" onClick={this.onOpenClick}>
        Open Drawer
      </Button>
    );

    const steps = [
      {
        name: 'Information',
        component: <p>Information step content</p>,
        drawerPanelContent: panel1Content,
        drawerToggleButton: drawerToggleButton
      },
      {
        name: 'Configuration',
        steps: [
          {
            name: 'Substep A',
            component: <p>Substep A content</p>,
            drawerPanelContent: panel2Content,
            drawerToggleButton: drawerToggleButton
          },
          {
            name: 'Substep B',
            component: <p>Substep B content</p>,
            drawerPanelContent: panel2Content,
            drawerToggleButton: drawerToggleButton
          },
          {
            name: 'Substep C',
            component: <p>Substep C content</p>,
            drawerPanelContent: panel2Content,
            drawerToggleButton: drawerToggleButton
          }
        ]
      },
      {
        name: 'Additional',
        component: <p>Additional step content</p>,
        drawerPanelContent: panel3Content,
        drawerToggleButton: drawerToggleButton
      },
      {
        name: 'Review',
        component: <p>Review step content</p>,
        nextButtonText: 'Finish'
      }
    ];

    const title = 'Wizard with drawer';

    return (
      <React.Fragment>
        <Wizard
          height={400}
          isDrawerExpanded={isDrawerExpanded}
          hasDrawer
          navAriaLabel={`${title} steps`}
          steps={steps}
        />
      </React.Fragment>
    );
  }
}
```
