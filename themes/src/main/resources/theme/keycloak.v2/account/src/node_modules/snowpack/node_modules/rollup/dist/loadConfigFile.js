/*
  @license
	Rollup.js v2.77.0
	Fri, 15 Jul 2022 10:23:18 GMT - commit 87da8ef24f61d6601dc550026fc59f8066bbb95d

	https://github.com/rollup/rollup

	Released under the MIT License.
*/
'use strict';

require('path');
require('process');
require('url');
const loadConfigFile_js = require('./shared/loadConfigFile.js');
require('./shared/rollup.js');
require('./shared/mergeOptions.js');
require('tty');
require('perf_hooks');
require('crypto');
require('fs');
require('events');



module.exports = loadConfigFile_js.loadAndParseConfigFile;
//# sourceMappingURL=loadConfigFile.js.map
