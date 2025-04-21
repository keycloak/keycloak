/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TextContent } from '../../TextContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TextContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<TextContent children={<>ReactNode</>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
