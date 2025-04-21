/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TextListItem } from '../../TextListItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TextListItem should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<TextListItem children={<>ReactNode</>} className={"''"} component={'li'} />);
  expect(asFragment()).toMatchSnapshot();
});
