/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { StackItem } from '../../StackItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('StackItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<StackItem isFilled={false} children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
