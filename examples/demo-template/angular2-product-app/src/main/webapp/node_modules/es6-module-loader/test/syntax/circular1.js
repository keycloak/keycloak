import {fn2, variable2} from './circular2.js';

export var variable1 = 'test circular 1';

export { output as output2 } from './circular2.js';

fn2();

export var output;

export function fn1() {
  output = variable2;
}