import {inc, dec} from './actions';

window.onload = function() {
  let number = document.getElementById('number');
  let incBtn = document.getElementById('inc');
  let decBtn = document.getElementById('dec');

  incBtn.addEventListener('click', function() {
    number.innerHTML = inc(+number.innerHTML);
  }, false);

  decBtn.addEventListener('click', function() {
    number.innerHTML = dec(+number.innerHTML);
  }, false);
};
