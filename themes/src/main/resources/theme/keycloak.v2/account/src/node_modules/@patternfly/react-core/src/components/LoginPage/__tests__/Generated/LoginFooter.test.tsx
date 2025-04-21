/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { LoginFooter } from '../../LoginFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginFooter should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<LoginFooter children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
