/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { SimpleListGroup } from '../../SimpleListGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SimpleListGroup should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <SimpleListGroup children={<>ReactNode</>} className={"''"} titleClassName={"''"} title={''} id={"''"} />
  );
  expect(asFragment()).toMatchSnapshot();
});
