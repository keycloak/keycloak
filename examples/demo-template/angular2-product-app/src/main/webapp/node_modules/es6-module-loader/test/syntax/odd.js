import { even } from './even.js';

export function odd(n) {
  return n != 0 && even(n - 1);
}