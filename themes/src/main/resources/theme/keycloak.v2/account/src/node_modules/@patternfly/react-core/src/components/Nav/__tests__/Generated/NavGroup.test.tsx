/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { NavGroup } from '../../NavGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('NavGroup should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<NavGroup title={'string'} children={<>ReactNode</>} className={"''"} id={'string'} />);
  expect(asFragment()).toMatchSnapshot();
});
