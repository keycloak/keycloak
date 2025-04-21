/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverDialog } from '../../PopoverDialog';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverDialog should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverDialog position={'top'} className={'null'} children={<>ReactNode</>} />);
  expect(asFragment()).toMatchSnapshot();
});
