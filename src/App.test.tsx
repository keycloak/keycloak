import React from 'react';
import { I18nextProvider } from 'react-i18next';

import { render } from '@testing-library/react';
import { i18n } from './i18n';
import { App } from './App';

test('renders Welcome', () => {
  const { getByText } = render(
    <I18nextProvider i18n={i18n}>
      <App />
    </I18nextProvider>
  );
  const titleElement = getByText(/Welcome to React and react-i18next/i);
  expect(titleElement).toBeInTheDocument();
});
