var ob = {};

var max = 5230; // just before call stack breaks
var count = 0;

function g(o) {
 if (count++ < max) {
   o.k = {};
   g(o.k); 
 }
 else {
   o.k = 'cool'; 
 }
}

g(ob);
console.log(JSON.stringify(ob));
