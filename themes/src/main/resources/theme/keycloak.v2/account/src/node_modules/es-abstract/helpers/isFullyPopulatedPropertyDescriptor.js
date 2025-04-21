'use strict';

module.exports = function isFullyPopulatedPropertyDescriptor(ES, Desc) {
	return '[[Enumerable]]' in Desc
		&& '[[Configurable]]' in Desc
		&& (ES.IsAccessorDescriptor(Desc) || ES.IsDataDescriptor(Desc));
};
