/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ListItem } from '../../ListItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ListItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<ListItem children={<>ReactNode</>} />);
  expect(asFragment()).toMatchSnapshot();
});
