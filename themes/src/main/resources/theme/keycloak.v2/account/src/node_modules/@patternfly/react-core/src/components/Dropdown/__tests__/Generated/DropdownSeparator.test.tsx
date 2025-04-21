import React from 'react';

import { render } from '@testing-library/react';

import { DropdownSeparator } from '../../DropdownSeparator';
import { DropdownArrowContext } from '../../dropdownConstants';

describe('DropdownSeparator', () => {
  it('should match snapshot', () => {
    const { asFragment } = render(
      <DropdownArrowContext.Provider value={{ sendRef: jest.fn(), keyHandler: undefined }}>
        <DropdownSeparator className={"''"} onClick={() => console.log('clicked')} />
      </DropdownArrowContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
