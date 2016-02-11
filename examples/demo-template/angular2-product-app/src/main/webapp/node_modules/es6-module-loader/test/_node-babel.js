'use strict';

global.expect = require('expect.js');

require('./_helper');

require('regenerator/runtime');

global.System = require('../index').System;
global.System.transpiler = 'babel';

require('./system.normalize.spec');
require('./system.spec');

require('./custom-loader');
require('./custom-loader.spec');
