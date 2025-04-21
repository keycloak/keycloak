/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DrawerContent } from '../../DrawerContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DrawerContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DrawerContent className={"''"} children={<div>ReactNode</div>} panelContent={<div>test</div>} />
  );
  expect(asFragment()).toMatchSnapshot();
});
