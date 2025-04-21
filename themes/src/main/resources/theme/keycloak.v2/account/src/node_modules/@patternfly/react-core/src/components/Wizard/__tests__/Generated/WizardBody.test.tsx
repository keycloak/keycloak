/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { WizardBody } from '../../WizardBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardBody should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <WizardBody
      children={'any'}
      hasNoBodyPadding={false}
      aria-label={'null'}
      aria-labelledby={'string'}
      mainComponent={'div'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
