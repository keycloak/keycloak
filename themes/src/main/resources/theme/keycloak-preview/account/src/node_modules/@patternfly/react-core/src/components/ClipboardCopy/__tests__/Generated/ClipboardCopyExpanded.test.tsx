/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ClipboardCopyExpanded } from '../../ClipboardCopyExpanded';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ClipboardCopyExpanded should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ClipboardCopyExpanded
      className={"''"}
      children={<div>ReactNode</div>}
      onChange={(): any => undefined}
      isReadOnly={false}
      isCode={false}
    />
  );
  expect(view).toMatchSnapshot();
});
