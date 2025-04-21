---
id: Progress stepper
section: components
beta: true
---

## Demos

### Basic

```js
import React from 'react';
import { ProgressStepper, ProgressStep, Button, Stack, StackItem } from '@patternfly/react-core';

ProgressStepperDemo = () => {
  const [currentStep, setCurrentStep] = React.useState(0);

  const steps = [
    { title: 'First step', id: 'step1' },
    { title: 'Second step', id: 'step2' },
    { title: 'Third step', id: 'step3' },
    { title: 'Fourth step', id: 'step4' },
    { title: 'Fifth step', id: 'step5' }
  ];

  const onStepForward = event => {
    const next = currentStep + 1;
    setCurrentStep(next <= 5 ? next : 4);
  };

  const onStepBack = event => {
    const next = currentStep - 1;
    setCurrentStep(next > 0 ? next : 0);
  };

  return (
    <Stack>
      <StackItem>
        <Button onClick={onStepBack} isDisabled={currentStep === 0}>
          Step back
        </Button>{' '}
        <Button onClick={onStepForward} isDisabled={currentStep === 5}>
          Step forward
        </Button>
        <br />
        <br />
      </StackItem>
      <StackItem>
        <div className="pf-screen-reader" aria-live="polite">
          {steps[currentStep] && `On ${steps[currentStep].title}.`}
          {steps[currentStep - 1] && `${steps[currentStep - 1].title} was successful.`}
        </div>
        <ProgressStepper>
          {steps.map((step, index) => {
            let variant = 'pending';
            let ariaLabel = 'pending step';
            if (index < currentStep) {
              variant = 'success';
              ariaLabel = 'completed step, step with success';
            } else if (index === currentStep) {
              variant = 'info';
              ariaLabel = 'current step';
            }

            return (
              <ProgressStep
                id={index}
                titleId={step.id}
                key={index}
                variant={variant}
                isCurrent={index === currentStep}
                aria-label={ariaLabel}
              >
                {step.title}
              </ProgressStep>
            );
          })}
        </ProgressStepper>
      </StackItem>
    </Stack>
  );
};
```
