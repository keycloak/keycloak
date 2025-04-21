'use strict';

const generate = require('regjsgen').generate;
const parse = require('regjsparser').parse;
const regenerate = require('regenerate');
const unicodeMatchProperty = require('unicode-match-property-ecmascript');
const unicodeMatchPropertyValue = require('unicode-match-property-value-ecmascript');
const iuMappings = require('./data/iu-mappings.js');
const ESCAPE_SETS = require('./data/character-class-escape-sets.js');

function flatMap(array, callback) {
	const result = [];
	array.forEach(item => {
		const res = callback(item);
		if (Array.isArray(res)) {
			result.push.apply(result, res);
		} else {
			result.push(res);
		}
	});
	return result;
}

const SPECIAL_CHARS = new Set('\\^$.*+?()[]{}|'.split(''));

// Prepare a Regenerate set containing all code points, used for negative
// character classes (if any).
const UNICODE_SET = regenerate().addRange(0x0, 0x10FFFF);

// Prepare a Regenerate set containing all code points that are supposed to be
// matched by `/./u`. https://mths.be/es6#sec-atom
const DOT_SET_UNICODE = UNICODE_SET.clone() // all Unicode code points
	.remove(
		// minus `LineTerminator`s (https://mths.be/es6#sec-line-terminators):
		0x000A, // Line Feed <LF>
		0x000D, // Carriage Return <CR>
		0x2028, // Line Separator <LS>
		0x2029  // Paragraph Separator <PS>
	);

const getCharacterClassEscapeSet = (character, unicode, ignoreCase) => {
	if (unicode) {
		if (ignoreCase) {
			return ESCAPE_SETS.UNICODE_IGNORE_CASE.get(character);
		}
		return ESCAPE_SETS.UNICODE.get(character);
	}
	return ESCAPE_SETS.REGULAR.get(character);
};

const getUnicodeDotSet = (dotAll) => {
	return dotAll ? UNICODE_SET : DOT_SET_UNICODE;
};

const getUnicodePropertyValueSet = (property, value) => {
	const path = value ?
		`${ property }/${ value }` :
		`Binary_Property/${ property }`;
	try {
		return require(`regenerate-unicode-properties/${ path }.js`);
	} catch (exception) {
		throw new Error(
			`Failed to recognize value \`${ value }\` for property ` +
			`\`${ property }\`.`
		);
	}
};

const handleLoneUnicodePropertyNameOrValue = (value) => {
	// It could be a `General_Category` value or a binary property.
	// Note: `unicodeMatchPropertyValue` throws on invalid values.
	try {
		const property = 'General_Category';
		const category = unicodeMatchPropertyValue(property, value);
		return getUnicodePropertyValueSet(property, category);
	} catch (exception) {}
	// It’s not a `General_Category` value, so check if it’s a property
	// of strings.
	try {
		return getUnicodePropertyValueSet('Property_of_Strings', value);
	} catch (exception) {}
	// Lastly, check if it’s a binary property of single code points.
	// Note: `unicodeMatchProperty` throws on invalid properties.
	const property = unicodeMatchProperty(value);
	return getUnicodePropertyValueSet(property);
};

const getUnicodePropertyEscapeSet = (value, isNegative) => {
	const parts = value.split('=');
	const firstPart = parts[0];
	let set;
	if (parts.length == 1) {
		set = handleLoneUnicodePropertyNameOrValue(firstPart);
	} else {
		// The pattern consists of two parts, i.e. `Property=Value`.
		const property = unicodeMatchProperty(firstPart);
		const value = unicodeMatchPropertyValue(property, parts[1]);
		set = getUnicodePropertyValueSet(property, value);
	}
	if (isNegative) {
		if (set.strings) {
			throw new Error('Cannot negate Unicode property of strings');
		}
		return {
			characters: UNICODE_SET.clone().remove(set.characters),
			strings: new Set()
		};
	}
	return {
		characters: set.characters.clone(),
		strings: set.strings
			// We need to escape strings like *️⃣ to make sure that they can be safelu used in unions
			? new Set(set.strings.map(str => SPECIAL_CHARS.has(str[0]) ? `\\${str}` : str))
			: new Set()
	};
};

const getUnicodePropertyEscapeCharacterClassData = (property, isNegative) => {
	const set = getUnicodePropertyEscapeSet(property, isNegative);
	const data = getCharacterClassEmptyData();
	data.singleChars = set.characters;
	if (set.strings.size > 0) {
		data.longStrings = set.strings;
		data.maybeIncludesStrings = true;
	}
	return data;
};

// Given a range of code points, add any case-folded code points in that range
// to a set.
regenerate.prototype.iuAddRange = function(min, max) {
	const $this = this;
	do {
		const folded = caseFold(min);
		if (folded) {
			$this.add(folded);
		}
	} while (++min <= max);
	return $this;
};
regenerate.prototype.iuRemoveRange = function(min, max) {
	const $this = this;
	do {
		const folded = caseFold(min);
		if (folded) {
			$this.remove(folded);
		}
	} while (++min <= max);
	return $this;
};

const update = (item, pattern) => {
	let tree = parse(pattern, config.useUnicodeFlag ? 'u' : '');
	switch (tree.type) {
		case 'characterClass':
		case 'group':
		case 'value':
			// No wrapping needed.
			break;
		default:
			// Wrap the pattern in a non-capturing group.
			tree = wrap(tree, pattern);
	}
	Object.assign(item, tree);
};

const wrap = (tree, pattern) => {
	// Wrap the pattern in a non-capturing group.
	return {
		'type': 'group',
		'behavior': 'ignore',
		'body': [tree],
		'raw': `(?:${ pattern })`
	};
};

const caseFold = (codePoint) => {
	return iuMappings.get(codePoint) || false;
};

const buildHandler = (action) => {
	switch (action) {
		case 'union':
			return {
				single: (data, cp) => {
					data.singleChars.add(cp);
				},
				regSet: (data, set2) => {
					data.singleChars.add(set2);
				},
				range: (data, start, end) => {
					data.singleChars.addRange(start, end);
				},
				iuRange: (data, start, end) => {
					data.singleChars.iuAddRange(start, end);
				},
				nested: (data, nestedData) => {
					data.singleChars.add(nestedData.singleChars);
					for (const str of nestedData.longStrings) data.longStrings.add(str);
					if (nestedData.maybeIncludesStrings) data.maybeIncludesStrings = true;
				}
			};
		case 'union-negative': {
			const regSet = (data, set2) => {
				data.singleChars = UNICODE_SET.clone().remove(set2).add(data.singleChars);
			};
			return {
				single: (data, cp) => {
					const unicode = UNICODE_SET.clone();
					data.singleChars = data.singleChars.contains(cp) ? unicode : unicode.remove(cp);
				},
				regSet: regSet,
				range: (data, start, end) => {
					data.singleChars = UNICODE_SET.clone().removeRange(start, end).add(data.singleChars);
				},
				iuRange: (data, start, end) => {
					data.singleChars = UNICODE_SET.clone().iuRemoveRange(start, end).add(data.singleChars);
				},
				nested: (data, nestedData) => {
					regSet(data, nestedData.singleChars);
					if (nestedData.maybeIncludesStrings) throw new Error('ASSERTION ERROR');
				}
			};
		}
		case 'intersection': {
			const regSet = (data, set2) => {
				if (data.first) data.singleChars = set2;
				else data.singleChars.intersection(set2);
			};
			return {
				single: (data, cp) => {
					data.singleChars = data.first || data.singleChars.contains(cp) ? regenerate(cp) : regenerate();
					data.longStrings.clear();
					data.maybeIncludesStrings = false;
				},
				regSet: (data, set) => {
					regSet(data, set);
					data.longStrings.clear();
					data.maybeIncludesStrings = false;
				},
				range: (data, start, end) => {
					if (data.first) data.singleChars.addRange(start, end);
					else data.singleChars.intersection(regenerate().addRange(start, end));
					data.longStrings.clear();
					data.maybeIncludesStrings = false;
				},
				iuRange: (data, start, end) => {
					if (data.first) data.singleChars.iuAddRange(start, end);
					else data.singleChars.intersection(regenerate().iuAddRange(start, end));
					data.longStrings.clear();
					data.maybeIncludesStrings = false;
				},
				nested: (data, nestedData) => {
					regSet(data, nestedData.singleChars);

					if (data.first) {
						data.longStrings = nestedData.longStrings;
						data.maybeIncludesStrings = nestedData.maybeIncludesStrings;
					} else {
						for (const str of data.longStrings) {
							if (!nestedData.longStrings.has(str)) data.longStrings.delete(str);
						}
						if (!nestedData.maybeIncludesStrings) data.maybeIncludesStrings = false;
					}
				}
			};
		}
		case 'subtraction': {
			const regSet = (data, set2) => {
				if (data.first) data.singleChars.add(set2);
				else data.singleChars.remove(set2);
			};
			return {
				single: (data, cp) => {
					if (data.first) data.singleChars.add(cp);
					else data.singleChars.remove(cp);
				},
				regSet: regSet,
				range: (data, start, end) => {
					if (data.first) data.singleChars.addRange(start, end);
					else data.singleChars.removeRange(start, end);
				},
				iuRange: (data, start, end) => {
					if (data.first) data.singleChars.iuAddRange(start, end);
					else data.singleChars.iuRemoveRange(start, end);
				},
				nested: (data, nestedData) => {
					regSet(data, nestedData.singleChars);

					if (data.first) {
						data.longStrings = nestedData.longStrings;
						data.maybeIncludesStrings = nestedData.maybeIncludesStrings;
					} else {
						for (const str of data.longStrings) {
							if (nestedData.longStrings.has(str)) data.longStrings.delete(str);
						}
					}
				}
			};
		}
		// The `default` clause is only here as a safeguard; it should never be
		// reached. Code coverage tools should ignore it.
		/* istanbul ignore next */
		default:
			throw new Error(`Unknown set action: ${ characterClassItem.kind }`);
	}
};

const getCharacterClassEmptyData = () => ({
	transformed: config.transform.unicodeFlag,
	singleChars: regenerate(),
	longStrings: new Set(),
	hasEmptyString: false,
	first: true,
	maybeIncludesStrings: false
});

const maybeFold = (codePoint) => {
	if (config.flags.ignoreCase && config.transform.unicodeFlag) {
		const folded = caseFold(codePoint);
		if (folded) {
			return [codePoint, folded];
		}
	}
	return [codePoint];
};

const computeClassStrings = (classStrings, regenerateOptions) => {
	let data = getCharacterClassEmptyData();

	for (const string of classStrings.strings) {
		if (string.characters.length === 1) {
			maybeFold(string.characters[0].codePoint).forEach((cp) => {
				data.singleChars.add(cp);
			});
		} else {
			let stringifiedString;
			if (config.flags.ignoreCase && config.transform.unicodeFlag) {
				stringifiedString = '';
				for (const ch of string.characters) {
					let set = regenerate(ch.codePoint);
					const folded = caseFold(ch.codePoint);
					if (folded) set.add(folded);
					stringifiedString += set.toString(regenerateOptions);
				}
			} else {
				stringifiedString = string.characters.map(ch => generate(ch)).join('')
			}

			data.longStrings.add(stringifiedString);
			data.maybeIncludesStrings = true;
		}
	}

	return data;
}

const computeCharacterClass = (characterClassItem, regenerateOptions) => {
	let data = getCharacterClassEmptyData();

	let handlePositive;
	let handleNegative;

	switch (characterClassItem.kind) {
		case 'union':
			handlePositive = buildHandler('union');
			handleNegative = buildHandler('union-negative');
			break;
		case 'intersection':
			handlePositive = buildHandler('intersection');
			handleNegative = buildHandler('subtraction');
			break;
		case 'subtraction':
			handlePositive = buildHandler('subtraction');
			handleNegative = buildHandler('intersection');
			break;
		// The `default` clause is only here as a safeguard; it should never be
		// reached. Code coverage tools should ignore it.
		/* istanbul ignore next */
		default:
			throw new Error(`Unknown character class kind: ${ characterClassItem.kind }`);
	}

	for (const item of characterClassItem.body) {
		switch (item.type) {
			case 'value':
				maybeFold(item.codePoint).forEach((cp) => {
					handlePositive.single(data, cp);
				});
				break;
			case 'characterClassRange':
				const min = item.min.codePoint;
				const max = item.max.codePoint;
				handlePositive.range(data, min, max);
				if (config.flags.ignoreCase && config.transform.unicodeFlag) {
					handlePositive.iuRange(data, min, max);
				}
				break;
			case 'characterClassEscape':
				handlePositive.regSet(data, getCharacterClassEscapeSet(
					item.value,
					config.flags.unicode,
					config.flags.ignoreCase
				));
				break;
			case 'unicodePropertyEscape':
				const nestedData = getUnicodePropertyEscapeCharacterClassData(item.value, item.negative);
				handlePositive.nested(data, nestedData);
				data.transformed =
					data.transformed ||
					config.transform.unicodePropertyEscapes ||
					(config.transform.unicodeSetsFlag && nestedData.maybeIncludesStrings);
				break;
			case 'characterClass':
				const handler = item.negative ? handleNegative : handlePositive;
				const res = computeCharacterClass(item, regenerateOptions);
				handler.nested(data, res);
				data.transformed = true;
				break;
			case 'classStrings':
				handlePositive.nested(data, computeClassStrings(item, regenerateOptions));
				data.transformed = true;
				break;
			// The `default` clause is only here as a safeguard; it should never be
			// reached. Code coverage tools should ignore it.
			/* istanbul ignore next */
			default:
				throw new Error(`Unknown term type: ${ item.type }`);
		}

		data.first = false;
	}

	if (characterClassItem.negative && data.maybeIncludesStrings) {
		throw new SyntaxError('Cannot negate set containing strings');
	}

	return data;
}

const processCharacterClass = (
	characterClassItem,
	regenerateOptions,
	computed = computeCharacterClass(characterClassItem, regenerateOptions)
) => {
	const negative = characterClassItem.negative;
	const { singleChars, transformed, longStrings } = computed;
	if (transformed) {
		const setStr = singleChars.toString(regenerateOptions);

		if (negative) {
			if (config.useUnicodeFlag) {
				update(characterClassItem, `[^${setStr[0] === '[' ? setStr.slice(1, -1) : setStr}]`)
			} else {
				update(characterClassItem, `(?!${setStr})[\\s\\S]`)
			}
		} else {
			const hasEmptyString = longStrings.has('');
			const pieces = Array.from(longStrings).sort((a, b) => b.length - a.length);

			if (setStr !== '[]' || longStrings.size === 0) {
				pieces.splice(pieces.length - (hasEmptyString ? 1 : 0), 0, setStr);
			}

			update(characterClassItem, pieces.join('|'));
		}
	}
	return characterClassItem;
};

const assertNoUnmatchedReferences = (groups) => {
	const unmatchedReferencesNames = Object.keys(groups.unmatchedReferences);
	if (unmatchedReferencesNames.length > 0) {
		throw new Error(`Unknown group names: ${unmatchedReferencesNames}`);
	}
};

const processTerm = (item, regenerateOptions, groups) => {
	switch (item.type) {
		case 'dot':
			if (config.transform.unicodeFlag) {
				update(
					item,
					getUnicodeDotSet(config.flags.dotAll).toString(regenerateOptions)
				);
			} else if (config.transform.dotAllFlag) {
				// TODO: consider changing this at the regenerate level.
				update(item, '[\\s\\S]');
			}
			break;
		case 'characterClass':
			item = processCharacterClass(item, regenerateOptions);
			break;
		case 'unicodePropertyEscape':
			const data = getUnicodePropertyEscapeCharacterClassData(item.value, item.negative);
			if (data.maybeIncludesStrings) {
				if (!config.flags.unicodeSets) {
					throw new Error(
						'Properties of strings are only supported when using the unicodeSets (v) flag.'
					);
				}
				if (config.transform.unicodeSetsFlag) {
					data.transformed = true;
					item = processCharacterClass(item, regenerateOptions, data);
				}
			} else if (config.transform.unicodePropertyEscapes) {
				update(
					item,
					data.singleChars.toString(regenerateOptions)
				);
			}
			break;
		case 'characterClassEscape':
			if (config.transform.unicodeFlag) {
				update(
					item,
					getCharacterClassEscapeSet(
						item.value,
						/* config.transform.unicodeFlag implies config.flags.unicode */ true,
						config.flags.ignoreCase
					).toString(regenerateOptions)
				);
			}
			break;
		case 'group':
			if (item.behavior == 'normal') {
				groups.lastIndex++;
			}
			if (item.name && config.transform.namedGroups) {
				const name = item.name.value;

				if (groups.namesConflicts[name]) {
					throw new Error(
						`Group '${ name }' has already been defined in this context.`
					);
				}
				groups.namesConflicts[name] = true;

				const index = groups.lastIndex;
				delete item.name;

				if (!groups.names[name]) {
					groups.names[name] = [];
				}
				groups.names[name].push(index);

				if (groups.onNamedGroup) {
					groups.onNamedGroup.call(null, name, index);
				}

				if (groups.unmatchedReferences[name]) {
					delete groups.unmatchedReferences[name];
				}
			}
			/* falls through */
		case 'quantifier':
			item.body = item.body.map(term => {
				return processTerm(term, regenerateOptions, groups);
			});
			break;
		case 'disjunction':
			const outerNamesConflicts = groups.namesConflicts;
			item.body = item.body.map(term => {
				groups.namesConflicts = Object.create(outerNamesConflicts);
				return processTerm(term, regenerateOptions, groups);
			});
			break;
		case 'alternative':
			item.body = flatMap(item.body, term => {
				const res = processTerm(term, regenerateOptions, groups);
				// Alternatives cannot contain alternatives; flatten them.
				return res.type === 'alternative' ? res.body : res;
			});
			break;
		case 'value':
			const codePoint = item.codePoint;
			const set = regenerate(codePoint);
			if (config.flags.ignoreCase && config.transform.unicodeFlag) {
				const folded = caseFold(codePoint);
				if (folded) {
					set.add(folded);
				}
			}
			update(item, set.toString(regenerateOptions));
			break;
		case 'reference':
			if (item.name) {
				const name = item.name.value;
				const indexes = groups.names[name];
				if (indexes) {
					const body = indexes.map(index => ({
						'type': 'reference',
						'matchIndex': index,
						'raw': '\\' + index,
					}));
					if (body.length === 1) {
						return body[0];
					}
					return {
						'type': 'alternative',
						'body': body,
						'raw': body.map(term => term.raw).join(''),
					};
				}

				// This named reference comes before the group where it’s defined,
				// so it’s always an empty match.
				groups.unmatchedReferences[name] = true;
				return {
					'type': 'group',
					'behavior': 'ignore',
					'body': [],
					'raw': '(?:)',
				};
			}
			break;
		case 'anchor':
		case 'empty':
		case 'group':
			// Nothing to do here.
			break;
		// The `default` clause is only here as a safeguard; it should never be
		// reached. Code coverage tools should ignore it.
		/* istanbul ignore next */
		default:
			throw new Error(`Unknown term type: ${ item.type }`);
	}
	return item;
};

const config = {
	'flags': {
		'ignoreCase': false,
		'unicode': false,
		'unicodeSets': false,
		'dotAll': false,
	},
	'transform': {
		'dotAllFlag': false,
		'unicodeFlag': false,
		'unicodeSetsFlag': false,
		'unicodePropertyEscapes': false,
		'namedGroups': false,
	},
	get useUnicodeFlag() {
		return (this.flags.unicode || this.flags.unicodeSets) && !this.transform.unicodeFlag;
	}
};

const validateOptions = (options) => {
	if (!options) return;

	for (const key of Object.keys(options)) {
		const value = options[key];
		switch (key) {
			case 'dotAllFlag':
			case 'unicodeFlag':
			case 'unicodePropertyEscapes':
			case 'namedGroups':
				if (value != null && value !== false && value !== 'transform') {
					throw new Error(`.${key} must be false (default) or 'transform'.`);
				}
				break;
			case 'unicodeSetsFlag':
				if (value != null && value !== false && value !== 'parse' && value !== 'transform') {
					throw new Error(`.${key} must be false (default), 'parse' or 'transform'.`);
				}
				break;
			case 'onNamedGroup':
				if (value != null && typeof value !== 'function') {
					throw new Error('.onNamedGroup must be a function.');
				}
				break;
			default:
				throw new Error(`.${key} is not a valid regexpu-core option.`);
		}
	}
};

const hasFlag = (flags, flag) => flags ? flags.includes(flag) : false;
const transform = (options, name) => options ? options[name] === 'transform' : false;

const rewritePattern = (pattern, flags, options) => {
	validateOptions(options);

	config.flags.unicode = hasFlag(flags, 'u');
	config.flags.unicodeSets = hasFlag(flags, 'v');
	config.flags.ignoreCase = hasFlag(flags, 'i');
	config.flags.dotAll = hasFlag(flags, 's');

	config.transform.dotAllFlag = config.flags.dotAll && transform(options, 'dotAllFlag');
	config.transform.unicodeFlag = (config.flags.unicode || config.flags.unicodeSets) && transform(options, 'unicodeFlag');
	config.transform.unicodeSetsFlag = config.flags.unicodeSets && transform(options, 'unicodeSetsFlag');

	// unicodeFlag: 'transform' implies unicodePropertyEscapes: 'transform'
	config.transform.unicodePropertyEscapes = config.flags.unicode && (
		transform(options, 'unicodeFlag') || transform(options, 'unicodePropertyEscapes')
	);
	config.transform.namedGroups = transform(options, 'namedGroups');

	const regjsparserFeatures = {
		'unicodeSet': Boolean(options && options.unicodeSetsFlag),

		// Enable every stable RegExp feature by default
		'unicodePropertyEscape': true,
		'namedGroups': true,
		'lookbehind': true,
	};

	const regenerateOptions = {
		'hasUnicodeFlag': config.useUnicodeFlag,
		'bmpOnly': !config.flags.unicode
	};

	const groups = {
		'onNamedGroup': options && options.onNamedGroup,
		'lastIndex': 0,
		'names': Object.create(null), // { [name]: Array<index> }
		'namesConflicts': Object.create(null), // { [name]: true }
		'unmatchedReferences': Object.create(null) // { [name]: true }
	};

	const tree = parse(pattern, flags, regjsparserFeatures);
	// Note: `processTerm` mutates `tree` and `groups`.
	processTerm(tree, regenerateOptions, groups);
	assertNoUnmatchedReferences(groups);
	return generate(tree);
};

module.exports = rewritePattern;
