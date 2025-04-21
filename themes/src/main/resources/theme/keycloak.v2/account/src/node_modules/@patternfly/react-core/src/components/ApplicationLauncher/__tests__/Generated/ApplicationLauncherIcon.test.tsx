/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ApplicationLauncherIcon } from '../../ApplicationLauncherIcon';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ApplicationLauncherIcon should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<ApplicationLauncherIcon children={<div>ReactNode</div>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
