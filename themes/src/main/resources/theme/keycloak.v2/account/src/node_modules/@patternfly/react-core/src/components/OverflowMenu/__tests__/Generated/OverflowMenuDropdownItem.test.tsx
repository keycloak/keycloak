import React from 'react';

import { render } from '@testing-library/react';

import { OverflowMenuDropdownItem } from '../../OverflowMenuDropdownItem';
import { DropdownArrowContext } from '../../../Dropdown';

describe('OverflowMenuDropdownItem', () => {
  it('should match snapshot', () => {
    const { asFragment } = render(
      <DropdownArrowContext.Provider value={{ sendRef: jest.fn(), keyHandler: undefined }}>
        <OverflowMenuDropdownItem children={'any'} isShared={false} />
      </DropdownArrowContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
