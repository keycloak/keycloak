import * as React from 'react';
import { mount } from 'enzyme';
import { Wizard, WizardStepFunctionType, WizardStep } from '../Wizard';

test('Wizard should match snapshot', () => {
  const steps: WizardStep[] = [
    { name: 'A', component: <p>Step 1</p> },
    {
      name: 'B',
      steps: [
        {
          name: 'B-1',
          component: <p>Step 2</p>,
          enableNext: true
        },
        {
          name: 'B-2',
          component: <p>Step 3</p>,
          enableNext: false
        }
      ]
    },
    { name: 'C', component: <p>Step 4</p> },
    { name: 'D', component: <p>Step 5</p> }
  ];
  const onBack: WizardStepFunctionType = step => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const name = { step };
  };
  const view = mount(
    <Wizard isOpen title="Wizard title" description="Description here" steps={steps} startAtStep={1} onBack={onBack} />
  );
  // ran into: https://github.com/airbnb/enzyme/issues/1213
  // so instead of: expect(view).toMatchSnapshot();
  const fragment = view.instance().render();
  expect(mount(<div>{fragment}</div>).getElement()).toMatchSnapshot();
});
