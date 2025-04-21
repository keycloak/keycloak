/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { SplitItem } from '../../SplitItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SplitItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<SplitItem isFilled={false} children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
