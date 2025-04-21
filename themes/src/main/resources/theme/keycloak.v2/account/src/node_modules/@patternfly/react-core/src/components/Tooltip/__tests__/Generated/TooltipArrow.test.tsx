/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TooltipArrow } from '../../TooltipArrow';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TooltipArrow should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<TooltipArrow className={'string'} />);
  expect(asFragment()).toMatchSnapshot();
});
