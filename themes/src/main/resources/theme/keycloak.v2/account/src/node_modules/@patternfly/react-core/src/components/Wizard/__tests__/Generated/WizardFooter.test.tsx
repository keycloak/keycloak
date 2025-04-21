/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { WizardFooter } from '../../WizardFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardFooter should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<WizardFooter children={'any'} />);
  expect(asFragment()).toMatchSnapshot();
});
