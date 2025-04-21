/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ApplicationLauncherContent } from '../../ApplicationLauncherContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ApplicationLauncherContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<ApplicationLauncherContent children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
