/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ModalBoxHeader } from '../../ModalBoxHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ModalBoxHeader should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ModalBoxHeader children={<>ReactNode</>} className={"''"} hideTitle={false} headingLevel={'h1'} />
  );
  expect(view).toMatchSnapshot();
});
