function inc(value) { // Play with me
  return value + 1;
}

function dec(value) { // and me
  return value - 1;
}

// IMPORTANT: it won't work as expected if you'll use addEventListener
// or $(document).ready as it will bind events again and again...
// So you may need some workaround for this or split modules in a particular way
window.onload = function() {
  var number = document.getElementById('number');
  var incBtn = document.getElementById('inc');
  var decBtn = document.getElementById('dec');

  incBtn.addEventListener('click', function() {
    number.innerHTML = inc(+number.innerHTML);
  }, false);

  decBtn.addEventListener('click', function() {
    number.innerHTML = dec(+number.innerHTML);
  }, false);
};
