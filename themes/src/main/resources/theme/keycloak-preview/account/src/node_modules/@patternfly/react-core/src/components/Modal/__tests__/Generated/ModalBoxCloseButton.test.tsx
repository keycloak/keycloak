/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ModalBoxCloseButton } from '../../ModalBoxCloseButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ModalBoxCloseButton should match snapshot (auto-generated)', () => {
  const view = shallow(<ModalBoxCloseButton className={"''"} onClose={() => undefined as any} />);
  expect(view).toMatchSnapshot();
});
