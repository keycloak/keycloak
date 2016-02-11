import { odd } from './odd.js'

export var counter = 0;

export function even(n) {
  counter++;
  return n == 0 || odd(n - 1);
}

odd(1);