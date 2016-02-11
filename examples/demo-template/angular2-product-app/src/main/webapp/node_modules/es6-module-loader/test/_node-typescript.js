'use strict';

global.expect = require('expect.js');

require('./_helper');

global.System = require('../index').System;
global.ts = require('typescript');
global.System.transpiler = 'typescript';

require('./system.spec');

require('./custom-loader');
require('./custom-loader.spec');
