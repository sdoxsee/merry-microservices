import React from 'react';
import { render } from '@testing-library/react';
import App from './App';

test('renders h1 title', () => {
  const { getByText } = render(<App />);
  const titleElement = getByText(/CRUD App with Hooks/i);
  expect(titleElement).toBeInTheDocument();
});
