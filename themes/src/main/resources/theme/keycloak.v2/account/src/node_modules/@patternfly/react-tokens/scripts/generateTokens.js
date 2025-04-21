const glob = require('glob');
const { dirname, basename, sep } = require('path');
const { parse, stringify } = require('css');
const { readFileSync } = require('fs');

const pfStylesDir = dirname(require.resolve('@patternfly/patternfly/patternfly.css'));

// Helpers
const formatCustomPropertyName = key => key.replace('--pf-', '').replace(/-+/g, '_');

const getRegexMatches = (string, regex) => {
  const res = {};
  let matches;
  while ((matches = regex.exec(string))) {
    res[matches[1]] = matches[2].trim();
  }
  return res;
};

const getDeclarations = cssAst =>
  cssAst.stylesheet.rules
    .filter(
      node =>
        node.type === 'rule' &&
        !node.selectors.includes('.pf-t-dark') &&
        (!node.selectors || !node.selectors.some(item => item.includes('.pf-theme-dark'))) // exclude dark theme blocks since dark theme variable values override default token values
    )
    .map(node => node.declarations.filter(decl => decl.type === 'declaration'))
    .reduce((acc, val) => acc.concat(val), []); // flatten

const formatFilePathToName = filePath => {
  // const filePathArr = filePath.split('/');
  let prefix = '';
  if (filePath.includes('components/')) {
    prefix = 'c_';
  } else if (filePath.includes('layouts/')) {
    prefix = 'l_';
  }
  return `${prefix}${basename(filePath, '.css').replace(/-+/g, '_')}`;
};

const getLocalVarsMap = cssFiles => {
  const res = {};

  cssFiles.forEach(filePath => {
    const cssAst = parse(readFileSync(filePath, 'utf8'));

    getDeclarations(cssAst).forEach(({ property, value, parent }) => {
      if (property.startsWith('--pf')) {
        res[property] = {
          ...res[property],
          [parent.selectors[0]]: value
        };
      }
    });
  });

  return res;
};

/**
 * Generates tokens from CSS in node_modules/@patternfly/patternfly/**
 *
 * @returns {object} of form {
 *   c_about_modal_box: {
 *     ".pf-c-about-modal-box" : {
 *       "global_Color_100": {
 *         "name": "--pf-global--Color--100",
 *         "value": "#fff",
 *         "values": [
 *           "--pf-global--Color--light-100",
 *           "$pf-global--Color--light-100",
 *           "$pf-color-white",
 *           "#fff"
 *         ]
 *       },
 *     },
 *     ".pf-c-about-modal-box .pf-c-card": {}
 *   }
 * }
 */
function generateTokens() {
  const cssFiles = glob
    .sync(['{**/{components,layouts}/**/*.css', '**/patternfly-charts.css', '**/patternfly-variables.css}'].join(','), {
      cwd: pfStylesDir,
      ignore: ['assets/**'],
      absolute: true
    })
    // Sort to put variables and charts at END of list so getLocalVarsMap returns correct values
    .sort((a, b) => (a.split(sep).length < b.split(sep).length ? 1 : -1));

  // various lookup tables to resolve variables
  const variables = readFileSync(require.resolve('@patternfly/patternfly/base/_variables.scss'), 'utf8');
  const cssGlobalsToScssVarsMap = getRegexMatches(variables, /(--pf-.*):\s*(?:#{)?(\$?pf-[\w- _]+)}?;/g);

  // contains default values and mappings to colors.scss for color values
  const scssVariables = readFileSync(
    require.resolve('@patternfly/patternfly/sass-utilities/scss-variables.scss'),
    'utf8'
  );
  const scssVarsMap = getRegexMatches(scssVariables, /(\$.*):\s*([^;^!]+)/g);

  // contains default values and mappings to colors.scss for color values
  const scssColorVariables = readFileSync(require.resolve('@patternfly/patternfly/sass-utilities/colors.scss'), 'utf8');
  const scssColorsMap = getRegexMatches(scssColorVariables, /(\$.*):\s*([^\s]+)\s*(?:!default);/g);

  // contains default values and mappings to colors.scss for color values
  const cssGlobalVariablesAst = parse(
    readFileSync(require.resolve('@patternfly/patternfly/base/patternfly-variables.css'), 'utf8')
  );

  cssGlobalVariablesAst.stylesheet.rules = cssGlobalVariablesAst.stylesheet.rules.filter(
    node => !node.selectors || !node.selectors.some(item => item.includes('.pf-theme-dark'))
  );

  const cssGlobalVariablesMap = getRegexMatches(stringify(cssGlobalVariablesAst), /(--pf-[\w-]*):\s*([\w -_]+);/g);

  const combinedScssVarsColorsMap = {
    ...scssVarsMap,
    ...scssColorsMap
  };

  const getComputedCSSVarValue = (value, selector, varMap) =>
    value.replace(/var\(([\w-]*)(,.*)?\)/g, (full, m1, m2) => {
      if (m1.startsWith('--pf-global')) {
        if (varMap[m1]) {
          return varMap[m1] + (m2 || '');
        } else {
          return full;
        }
      } else {
        if (selector) {
          return getFromLocalVarsMap(m1, selector) + (m2 || '');
        }
      }
    });

  const getComputedScssVarValue = value =>
    value.replace(/\$pf[^,)\s*/]*/g, match => {
      if (combinedScssVarsColorsMap[match]) {
        return combinedScssVarsColorsMap[match];
      } else {
        return match;
      }
    });

  const getVarsMap = (value, selector) => {
    // evaluate the value and follow the variable chain
    const varsMap = [value];

    let computedValue = value;
    let finalValue = value;
    while (finalValue.includes('var(--pf') || computedValue.includes('var(--pf') || computedValue.includes('$pf-')) {
      // keep following the variable chain until we get to a value
      if (finalValue.includes('var(--pf')) {
        finalValue = getComputedCSSVarValue(finalValue, selector, cssGlobalVariablesMap);
      }
      if (computedValue.includes('var(--pf')) {
        computedValue = getComputedCSSVarValue(computedValue, selector, cssGlobalsToScssVarsMap);
      } else {
        computedValue = getComputedScssVarValue(computedValue);
      }

      // error out if variable doesn't exist to avoid infinite loop
      if (finalValue === value && computedValue === value) {
        // eslint-disable-next-line no-console
        console.error(`Error: "${value}" variable not found`);
        throw Error;
      }

      varsMap.push(computedValue);
    }
    const lastElement = varsMap[varsMap.length - 1];
    if (lastElement.includes('pf-')) {
      varsMap.push(finalValue);
    }
    // all values should not be boxed by var()
    return varsMap.map(variable => variable.replace(/var\(([\w-]*)\)/g, (_, match) => match));
  };

  // pre-populate the localVarsMap so we can lookup local variables within or across files, e.g. if we have the declaration:
  // --pf-c-chip-group--MarginBottom: calc(var(--pf-c-chip-group--c-chip--MarginBottom) * -1);
  // then we need to find:
  // --pf-c-chip-group--c-chip--MarginBottom: var(--pf-global--spacer--xs);
  const localVarsMap = getLocalVarsMap(cssFiles);

  const getFromLocalVarsMap = (match, selector) => {
    if (localVarsMap[match]) {
      // have exact selectors match
      if (localVarsMap[match][selector]) {
        return localVarsMap[match][selector];
      } else if (Object.keys(localVarsMap[match]).length === 1) {
        // only one match, return its value
        return Object.values(localVarsMap[match])[0];
      } else {
        // find the nearest parent selector and return its value
        let bestMatch = '';
        let bestValue = '';
        for (const key in localVarsMap[match]) {
          if (localVarsMap[match].hasOwnProperty(key)) {
            // remove trailing * from key to compare
            let sanitizedKey = key.replace(/\*$/, '').trim();
            sanitizedKey = sanitizedKey.replace(/>$/, '').trim();
            sanitizedKey = sanitizedKey.replace(/\[.*\]$/, '').trim();
            // is key a parent of selector?
            if (selector.indexOf(sanitizedKey) > -1) {
              if (sanitizedKey.length > bestMatch.length) {
                // longest matching key is the winner
                bestMatch = key;
                bestValue = localVarsMap[match][key];
              }
            }
          }
        }
        if (!bestMatch) {
          // eslint-disable-next-line no-console
          console.error(`no matching selector found for ${match} in localVarsMap`);
        }
        return bestValue;
      }
    } else {
      // eslint-disable-next-line no-console
      console.error(`no matching property found for ${match} in localVarsMap`);
    }
  };

  const fileTokens = {};
  cssFiles.forEach(filePath => {
    const cssAst = parse(readFileSync(filePath, 'utf8'));
    // key is the formatted file name, e.g. c_about_modal_box
    const key = formatFilePathToName(filePath);

    getDeclarations(cssAst)
      .filter(({ property }) => property.startsWith('--pf'))
      .forEach(({ property, value, parent }) => {
        const selector = parent.selectors[0];

        const varsMap = getVarsMap(value, selector);
        const propertyObj = {
          name: property,
          value: varsMap[varsMap.length - 1]
        };
        if (varsMap.length > 1) {
          propertyObj.values = varsMap;
        }

        fileTokens[key] = fileTokens[key] || {};
        fileTokens[key][selector] = fileTokens[key][selector] || {};
        fileTokens[key][selector][formatCustomPropertyName(property)] = propertyObj;
      });
  });

  return fileTokens;
}

module.exports = {
  generateTokens
};
