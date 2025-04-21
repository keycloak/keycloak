/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TooltipContent } from '../../TooltipContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TooltipContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <TooltipContent className={'string'} children={<div>ReactNode</div>} isLeftAligned={true} />
  );
  expect(asFragment()).toMatchSnapshot();
});
