/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OptionsMenuToggleWithText } from '../../OptionsMenuToggleWithText';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuToggleWithText should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OptionsMenuToggleWithText
      parentId={"''"}
      toggleText={<div>ReactNode</div>}
      toggleTextClassName={"''"}
      toggleButtonContents={<div>ReactNode</div>}
      toggleButtonContentsClassName={"''"}
      onToggle={() => null as any}
      onEnter={(event: React.MouseEvent<HTMLButtonElement>) => undefined as void}
      isOpen={false}
      isPlain={false}
      isFocused={false}
      isHovered={false}
      isActive={false}
      isDisabled={false}
      parentRef={document.body}
      ariaHasPopup={true}
      aria-label={"'Options menu'"}
    />
  );
  expect(view).toMatchSnapshot();
});
