/**
 * Parse the d property of an SVG path into an array of drawing commands.
 * @param  {String} d SvgPath d attribute.]
 * @return {Array} an array of drawing commands.
 */
export function parseSvgPath(d) { //jshint ignore:line
    'use strict';

    var commands = [];
    var commandTokens = ['M','L','I','H','V','C','S','Q','T','A'];
    var command;
    var in_x = false;
    var in_y = false;
    var x = '';
    var y = '';
    for(var i = 0; i <= d.length; i++) {
        if (commandTokens.indexOf(d[i]) !== -1) {
            if (in_x || in_y) {
                commands.push({command: command, x: x, y: y});
                x = '';
                y = '';
            }
            command = d[i];
            in_x = true;
            in_y = false;
        }
        else {
            if (d[i] === ',') {
                if (in_y) {
                    commands.push({command: command, x: x, y: y});
                    x = '';
                    y = '';
                }
                in_x = !in_x;
                in_y = !in_y;
            }
            else if (in_x) {
                x += d[i];
            }
            else if (in_y) {
                y += d[i];
            }
        }
    }
    if (d[i] !== ',' && in_y) {
        commands.push({command: command, x: x, y: y});
    }
    return commands;
}
