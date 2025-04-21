/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ProgressBar } from '../../ProgressBar';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ProgressBar should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<ProgressBar children={<>ReactNode</>} className={"''"} value={42} />);
  expect(asFragment()).toMatchSnapshot();
});
