/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { WizardNav } from '../../WizardNav';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardNav should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <WizardNav children={'any'} aria-label={'string'} aria-labelledby={'string'} isOpen={false} returnList={false} />
  );
  expect(asFragment()).toMatchSnapshot();
});
