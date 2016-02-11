var _ = require('lodash');

function sum(arr) {
    return _.reduce(arr, function(sum, num) {
        return sum + num;
    }, 0);
}

_.mixin({
    sum: sum
});
