
System.paths['*'] = 'base/*';
baseURL += 'base/';

System.paths.traceur = 'node_modules/traceur/bin/traceur.js';
System.paths.babel = 'node_modules/babel-core/browser.js';
System.paths.typescript = 'node_modules/typescript/bin/typescript.js';

System.transpiler = __karma__.config.system.transpiler;
