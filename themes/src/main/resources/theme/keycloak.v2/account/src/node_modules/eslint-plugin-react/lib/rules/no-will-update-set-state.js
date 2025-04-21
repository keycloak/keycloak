/**
 * @fileoverview Prevent usage of setState in componentWillUpdate
 * @author Yannick Croissant
 */

'use strict';

const makeNoMethodSetStateRule = require('../util/makeNoMethodSetStateRule');
const testReactVersion = require('../util/version').testReactVersion;

module.exports = makeNoMethodSetStateRule(
  'componentWillUpdate',
  (context) => testReactVersion(context, '>= 16.3.0')
);
