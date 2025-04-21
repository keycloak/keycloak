import React from 'react';
import { render } from '@testing-library/react';

import { ApplicationLauncherSeparator } from '../../ApplicationLauncherSeparator';
import { DropdownArrowContext } from '../../../Dropdown/dropdownConstants';

describe('ApplicationLauncherSeparator', () => {
  it('should match snapshot', () => {
    const { asFragment } = render(
      <DropdownArrowContext.Provider value={{ sendRef: jest.fn(), keyHandler: undefined }}>
        <ApplicationLauncherSeparator />
      </DropdownArrowContext.Provider>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
