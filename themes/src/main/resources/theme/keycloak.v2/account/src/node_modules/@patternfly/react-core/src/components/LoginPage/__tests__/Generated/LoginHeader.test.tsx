/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { LoginHeader } from '../../LoginHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginHeader should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<LoginHeader children={<>ReactNode</>} className={"''"} headerBrand={null} />);
  expect(asFragment()).toMatchSnapshot();
});
