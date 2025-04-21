/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { FlexItem } from '../../FlexItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FlexItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<FlexItem children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
