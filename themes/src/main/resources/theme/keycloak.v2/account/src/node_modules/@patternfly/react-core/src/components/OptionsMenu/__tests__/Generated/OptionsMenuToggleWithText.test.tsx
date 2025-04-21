/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { OptionsMenuToggleWithText } from '../../OptionsMenuToggleWithText';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuToggleWithText should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <OptionsMenuToggleWithText
      parentId={"''"}
      toggleText={<div>ReactNode</div>}
      toggleTextClassName={"''"}
      toggleButtonContents={<div>ReactNode</div>}
      toggleButtonContentsClassName={"''"}
      onToggle={() => null as any}
      isOpen={false}
      isPlain={false}
      isActive={false}
      isDisabled={false}
      aria-haspopup={true}
      aria-label={"'Options menu'"}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
