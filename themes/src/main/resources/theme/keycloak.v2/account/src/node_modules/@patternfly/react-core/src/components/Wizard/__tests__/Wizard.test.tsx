import React from 'react';
import { render, screen } from '@testing-library/react';
import { Wizard, WizardStepFunctionType, WizardStep } from '../Wizard';
import { DrawerPanelContent, DrawerColorVariant, DrawerHead } from '../../Drawer';

describe('Wizard', () => {
  test('Wizard should match snapshot', () => {
    const steps: WizardStep[] = [
      { name: 'A', id: 'step-A', component: <p>Step 1</p> },
      {
        name: 'B',
        id: 'step-B',
        steps: [
          {
            name: 'B-1',
            id: 'step-B-1',
            component: <p>Step 2</p>,
            enableNext: true
          },
          {
            name: 'B-2',
            id: 'step-B-2',
            component: <p>Step 3</p>,
            enableNext: false
          }
        ]
      },
      { name: 'C', id: 'step-C', component: <p>Step 4</p> },
      { name: 'D', id: 'step-D', component: <p>Step 5</p> }
    ];
    const onBack: WizardStepFunctionType = step => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const name = { step };
    };

    const { asFragment } = render(
      <Wizard title="Wizard title" description="Description here" steps={steps} startAtStep={1} onBack={onBack} />
    );

    expect(asFragment()).toMatchSnapshot();
  });

  test('Expandable Nav Wizard should match snapshot', () => {
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

    const { asFragment } = render(
      <Wizard
        title="Wizard title"
        description="Description here"
        steps={steps}
        startAtStep={1}
        onBack={onBack}
        isNavExpandable
      />
    );

    expect(asFragment()).toMatchSnapshot();
  });

  test('bare wiz', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];

    const { asFragment } = render(
      <Wizard
        description="This wizard uses anchor tags for the nav item elements"
        id="inPageWizWithAnchorsId"
        steps={steps}
      />
    );

    expect(asFragment()).toMatchSnapshot();
  });

  test('wiz with title', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];

    render(<Wizard title="Wizard" description="This wizard has a title" steps={steps} />);
    expect(screen.getByRole('heading', { name: 'Wizard' })).toBeInTheDocument();
  });

  test('wiz with drawer', () => {
    const wizDrawerPanelContent = (
      <DrawerPanelContent colorVariant={DrawerColorVariant.light200}>
        <DrawerHead>
          <span>This wizard has a drawer with drawer panel content</span>
        </DrawerHead>
      </DrawerPanelContent>
    );

    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p>, drawerPanelContent: wizDrawerPanelContent }];

    render(<Wizard title="Wizard with drawer" hasDrawer isDrawerExpanded steps={steps} />);
    expect(screen.getByRole('heading', { name: 'Wizard with drawer' })).toBeInTheDocument();
    expect(screen.getByText('This wizard has a drawer with drawer panel content')).toBeInTheDocument();
  });

  test('wiz with title and navAriaLabel combination', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];

    render(<Wizard title="Wizard" navAriaLabel="Some label" description="This wizard has a title" steps={steps} />);

    const nav = screen.getByLabelText('Some label');

    expect(screen.getByRole('heading', { name: 'Wizard' })).toBeInTheDocument();
    expect(nav).toBeInTheDocument();
    expect(nav).toHaveAttribute('aria-labelledby', 'pf-wizard-title-5');
  });

  test('wiz with title, navAriaLabel, and mainAriaLabel combination', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];
    render(
      <Wizard
        title="Wizard"
        navAriaLabel="nav aria-label"
        mainAriaLabel="main aria-label"
        description="This wizard has a title"
        steps={steps}
      />
    );

    expect(screen.getByRole('heading', { name: 'Wizard' })).toBeInTheDocument();
    expect(screen.getByLabelText('nav aria-label')).toBeInTheDocument();
    expect(screen.getByLabelText('main aria-label')).toBeInTheDocument();
  });

  test('wiz with navAriaLabel and mainAriaLabel but without title', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];

    render(
      <Wizard
        navAriaLabel="nav aria-label"
        mainAriaLabel="main aria-label"
        description="This wizard has a title"
        steps={steps}
      />
    );

    expect(screen.getByLabelText('nav aria-label')).toBeInTheDocument();
    expect(screen.getByLabelText('main aria-label')).toBeInTheDocument();
  });

  test('wiz with navAriaLabel and navAriaLabelledby and without title', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];

    render(
      <Wizard
        navAriaLabelledBy="nav-aria-labelledby"
        navAriaLabel="nav aria-label"
        mainAriaLabel="main aria-label"
        description="This wizard has a title"
        steps={steps}
      />
    );

    const nav = screen.getByLabelText('nav aria-label');

    expect(nav).toBeInTheDocument();
    expect(nav).toHaveAttribute('aria-labelledby', 'nav-aria-labelledby');
  });

  test('wiz with navAriaLabel and navAriaLabelledby and without title', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];
    render(
      <Wizard
        navAriaLabel="nav aria-label"
        mainAriaLabel="main aria-label"
        mainAriaLabelledBy="main-aria-labelledby"
        description="This wizard has a title"
        steps={steps}
      />
    );

    const main = screen.getByLabelText('main aria-label');

    expect(main).toBeInTheDocument();
    expect(main).toHaveAttribute('aria-labelledby', 'main-aria-labelledby');
  });

  test('wiz with title, navAriaLabelledBy, navAriaLabel, mainAriaLabel, and mainAriaLabelledby', () => {
    const steps: WizardStep[] = [{ name: 'A', component: <p>Step 1</p> }];
    render(
      <Wizard
        title="Wiz title"
        navAriaLabel="nav aria-label"
        navAriaLabelledBy="nav-aria-labelledby"
        mainAriaLabel="main aria-label"
        mainAriaLabelledBy="main-aria-labelledby"
        description="This wizard has a title"
        steps={steps}
      />
    );

    const nav = screen.getByLabelText('nav aria-label');
    expect(nav).toBeInTheDocument();
    expect(nav).toHaveAttribute('aria-labelledby', 'nav-aria-labelledby');

    const main = screen.getByLabelText('main aria-label');
    expect(main).toBeInTheDocument();
    expect(main).toHaveAttribute('aria-labelledby', 'main-aria-labelledby');
  });
});
