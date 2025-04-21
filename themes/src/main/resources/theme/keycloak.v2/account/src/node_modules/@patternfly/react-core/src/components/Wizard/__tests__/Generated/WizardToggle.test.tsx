/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { WizardToggle } from '../../WizardToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardToggle should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <WizardToggle
      nav={(_isWizardNavOpen: boolean) => undefined as React.ReactElement}
      steps={[]}
      activeStep={{ name: 'some step' }}
      children={<div>ReactNode</div>}
      hasNoBodyPadding={false}
      isNavOpen={true}
      onNavToggle={(isOpen: boolean) => undefined as void}
      aria-label={"'Wizard Toggle'"}
      mainAriaLabelledBy={'null'}
      mainAriaLabel={'null'}
      isInPage={true}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
