this.zxcvbnts = this.zxcvbnts || {};
this.zxcvbnts.core = (function (exports) {
    'use strict';

    const empty = obj => Object.keys(obj).length === 0;
    const extend = (listToExtend, list) => // eslint-disable-next-line prefer-spread
    listToExtend.push.apply(listToExtend, list);
    const translate = (string, chrMap) => {
      const tempArray = string.split('');
      return tempArray.map(char => chrMap[char] || char).join('');
    }; // mod implementation that works for negative numbers

    const sorted = matches => matches.sort((m1, m2) => m1.i - m2.i || m1.j - m2.j);
    const buildRankedDictionary = orderedList => {
      const result = {};
      let counter = 1; // rank starts at 1, not 0

      orderedList.forEach(word => {
        result[word] = counter;
        counter += 1;
      });
      return result;
    };

    var dateSplits = {
      4: [[1, 2], [2, 3]],
      5: [[1, 3], [2, 3]],
      6: [[1, 2], [2, 4], [4, 5]],
      7: [[1, 3], [2, 3], [4, 5], [4, 6]],
      8: [[2, 4], [4, 6]]
    };

    const DATE_MAX_YEAR = 2050;
    const DATE_MIN_YEAR = 1000;
    const DATE_SPLITS = dateSplits;
    const BRUTEFORCE_CARDINALITY = 10;
    const MIN_GUESSES_BEFORE_GROWING_SEQUENCE = 10000;
    const MIN_SUBMATCH_GUESSES_SINGLE_CHAR = 10;
    const MIN_SUBMATCH_GUESSES_MULTI_CHAR = 50;
    const MIN_YEAR_SPACE = 20; // \xbf-\xdf is a range for almost all special uppercase letter like Ä and so on

    const START_UPPER = /^[A-Z\xbf-\xdf][^A-Z\xbf-\xdf]+$/;
    const END_UPPER = /^[^A-Z\xbf-\xdf]+[A-Z\xbf-\xdf]$/; // \xdf-\xff is a range for almost all special lowercase letter like ä and so on

    const ALL_UPPER = /^[A-Z\xbf-\xdf]+$/;
    const ALL_UPPER_INVERTED = /^[^a-z\xdf-\xff]+$/;
    const ALL_LOWER = /^[a-z\xdf-\xff]+$/;
    const ALL_LOWER_INVERTED = /^[^A-Z\xbf-\xdf]+$/;
    const ONE_UPPER = /[a-z\xdf-\xff]/;
    const ONE_LOWER = /[A-Z\xbf-\xdf]/;
    const ALPHA_INVERTED = /[^A-Za-z\xbf-\xdf]/gi;
    const ALL_DIGIT = /^\d+$/;
    const REFERENCE_YEAR = new Date().getFullYear();
    const REGEXEN = {
      recentYear: /19\d\d|200\d|201\d|202\d/g
    };

    /*
     * -------------------------------------------------------------------------------
     *  date matching ----------------------------------------------------------------
     * -------------------------------------------------------------------------------
     */

    class MatchDate {
      /*
       * a "date" is recognized as:
       *   any 3-tuple that starts or ends with a 2- or 4-digit year,
       *   with 2 or 0 separator chars (1.1.91 or 1191),
       *   maybe zero-padded (01-01-91 vs 1-1-91),
       *   a month between 1 and 12,
       *   a day between 1 and 31.
       *
       * note: this isn't true date parsing in that "feb 31st" is allowed,
       * this doesn't check for leap years, etc.
       *
       * recipe:
       * start with regex to find maybe-dates, then attempt to map the integers
       * onto month-day-year to filter the maybe-dates into dates.
       * finally, remove matches that are substrings of other matches to reduce noise.
       *
       * note: instead of using a lazy or greedy regex to find many dates over the full string,
       * this uses a ^...$ regex against every substring of the password -- less performant but leads
       * to every possible date match.
       */
      match({
        password
      }) {
        const matches = [...this.getMatchesWithoutSeparator(password), ...this.getMatchesWithSeparator(password)];
        const filteredMatches = this.filterNoise(matches);
        return sorted(filteredMatches);
      }

      getMatchesWithSeparator(password) {
        const matches = [];
        const maybeDateWithSeparator = /^(\d{1,4})([\s/\\_.-])(\d{1,2})\2(\d{1,4})$/; // # dates with separators are between length 6 '1/1/91' and 10 '11/11/1991'

        for (let i = 0; i <= Math.abs(password.length - 6); i += 1) {
          for (let j = i + 5; j <= i + 9; j += 1) {
            if (j >= password.length) {
              break;
            }

            const token = password.slice(i, +j + 1 || 9e9);
            const regexMatch = maybeDateWithSeparator.exec(token);

            if (regexMatch != null) {
              const dmy = this.mapIntegersToDayMonthYear([parseInt(regexMatch[1], 10), parseInt(regexMatch[3], 10), parseInt(regexMatch[4], 10)]);

              if (dmy != null) {
                matches.push({
                  pattern: 'date',
                  token,
                  i,
                  j,
                  separator: regexMatch[2],
                  year: dmy.year,
                  month: dmy.month,
                  day: dmy.day
                });
              }
            }
          }
        }

        return matches;
      } // eslint-disable-next-line max-statements


      getMatchesWithoutSeparator(password) {
        const matches = [];
        const maybeDateNoSeparator = /^\d{4,8}$/;

        const metric = candidate => Math.abs(candidate.year - REFERENCE_YEAR); // # dates without separators are between length 4 '1191' and 8 '11111991'


        for (let i = 0; i <= Math.abs(password.length - 4); i += 1) {
          for (let j = i + 3; j <= i + 7; j += 1) {
            if (j >= password.length) {
              break;
            }

            const token = password.slice(i, +j + 1 || 9e9);

            if (maybeDateNoSeparator.exec(token)) {
              const candidates = [];
              const index = token.length;
              const splittedDates = DATE_SPLITS[index];
              splittedDates.forEach(([k, l]) => {
                const dmy = this.mapIntegersToDayMonthYear([parseInt(token.slice(0, k), 10), parseInt(token.slice(k, l), 10), parseInt(token.slice(l), 10)]);

                if (dmy != null) {
                  candidates.push(dmy);
                }
              });

              if (candidates.length > 0) {
                /*
                 * at this point: different possible dmy mappings for the same i,j substring.
                 * match the candidate date that likely takes the fewest guesses: a year closest
                 * to 2000.
                 * (scoring.REFERENCE_YEAR).
                 *
                 * ie, considering '111504', prefer 11-15-04 to 1-1-1504
                 * (interpreting '04' as 2004)
                 */
                let bestCandidate = candidates[0];
                let minDistance = metric(candidates[0]);
                candidates.slice(1).forEach(candidate => {
                  const distance = metric(candidate);

                  if (distance < minDistance) {
                    bestCandidate = candidate;
                    minDistance = distance;
                  }
                });
                matches.push({
                  pattern: 'date',
                  token,
                  i,
                  j,
                  separator: '',
                  year: bestCandidate.year,
                  month: bestCandidate.month,
                  day: bestCandidate.day
                });
              }
            }
          }
        }

        return matches;
      }
      /*
       * matches now contains all valid date strings in a way that is tricky to capture
       * with regexes only. while thorough, it will contain some unintuitive noise:
       *
       * '2015_06_04', in addition to matching 2015_06_04, will also contain
       * 5(!) other date matches: 15_06_04, 5_06_04, ..., even 2015 (matched as 5/1/2020)
       *
       * to reduce noise, remove date matches that are strict substrings of others
       */


      filterNoise(matches) {
        return matches.filter(match => {
          let isSubmatch = false;
          const matchesLength = matches.length;

          for (let o = 0; o < matchesLength; o += 1) {
            const otherMatch = matches[o];

            if (match !== otherMatch) {
              if (otherMatch.i <= match.i && otherMatch.j >= match.j) {
                isSubmatch = true;
                break;
              }
            }
          }

          return !isSubmatch;
        });
      }
      /*
       * given a 3-tuple, discard if:
       *   middle int is over 31 (for all dmy formats, years are never allowed in the middle)
       *   middle int is zero
       *   any int is over the max allowable year
       *   any int is over two digits but under the min allowable year
       *   2 integers are over 31, the max allowable day
       *   2 integers are zero
       *   all integers are over 12, the max allowable month
       */
      // eslint-disable-next-line complexity, max-statements


      mapIntegersToDayMonthYear(integers) {
        if (integers[1] > 31 || integers[1] <= 0) {
          return null;
        }

        let over12 = 0;
        let over31 = 0;
        let under1 = 0;

        for (let o = 0, len1 = integers.length; o < len1; o += 1) {
          const int = integers[o];

          if (int > 99 && int < DATE_MIN_YEAR || int > DATE_MAX_YEAR) {
            return null;
          }

          if (int > 31) {
            over31 += 1;
          }

          if (int > 12) {
            over12 += 1;
          }

          if (int <= 0) {
            under1 += 1;
          }
        }

        if (over31 >= 2 || over12 === 3 || under1 >= 2) {
          return null;
        }

        return this.getDayMonth(integers);
      } // eslint-disable-next-line max-statements


      getDayMonth(integers) {
        // first look for a four digit year: yyyy + daymonth or daymonth + yyyy
        const possibleYearSplits = [[integers[2], integers.slice(0, 2)], [integers[0], integers.slice(1, 3)] // year first
        ];
        const possibleYearSplitsLength = possibleYearSplits.length;

        for (let j = 0; j < possibleYearSplitsLength; j += 1) {
          const [y, rest] = possibleYearSplits[j];

          if (DATE_MIN_YEAR <= y && y <= DATE_MAX_YEAR) {
            const dm = this.mapIntegersToDayMonth(rest);

            if (dm != null) {
              return {
                year: y,
                month: dm.month,
                day: dm.day
              };
            }
            /*
             * for a candidate that includes a four-digit year,
             * when the remaining integers don't match to a day and month,
             * it is not a date.
             */


            return null;
          }
        } // given no four-digit year, two digit years are the most flexible int to match, so
        // try to parse a day-month out of integers[0..1] or integers[1..0]


        for (let k = 0; k < possibleYearSplitsLength; k += 1) {
          const [y, rest] = possibleYearSplits[k];
          const dm = this.mapIntegersToDayMonth(rest);

          if (dm != null) {
            return {
              year: this.twoToFourDigitYear(y),
              month: dm.month,
              day: dm.day
            };
          }
        }

        return null;
      }

      mapIntegersToDayMonth(integers) {
        const temp = [integers, integers.slice().reverse()];

        for (let i = 0; i < temp.length; i += 1) {
          const data = temp[i];
          const day = data[0];
          const month = data[1];

          if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
            return {
              day,
              month
            };
          }
        }

        return null;
      }

      twoToFourDigitYear(year) {
        if (year > 99) {
          return year;
        }

        if (year > 50) {
          // 87 -> 1987
          return year + 1900;
        } // 15 -> 2015


        return year + 2000;
      }

    }

    const peq = new Uint32Array(0x10000);
    const myers_32 = (a, b) => {
      const n = a.length;
      const m = b.length;
      const lst = 1 << (n - 1);
      let pv = -1;
      let mv = 0;
      let sc = n;
      let i = n;
      while (i--) {
        peq[a.charCodeAt(i)] |= 1 << i;
      }
      for (i = 0; i < m; i++) {
        let eq = peq[b.charCodeAt(i)];
        const xv = eq | mv;
        eq |= ((eq & pv) + pv) ^ pv;
        mv |= ~(eq | pv);
        pv &= eq;
        if (mv & lst) {
          sc++;
        }
        if (pv & lst) {
          sc--;
        }
        mv = (mv << 1) | 1;
        pv = (pv << 1) | ~(xv | mv);
        mv &= xv;
      }
      i = n;
      while (i--) {
        peq[a.charCodeAt(i)] = 0;
      }
      return sc;
    };

    const myers_x = (a, b) => {
      const n = a.length;
      const m = b.length;
      const mhc = [];
      const phc = [];
      const hsize = Math.ceil(n / 32);
      const vsize = Math.ceil(m / 32);
      let score = m;
      for (let i = 0; i < hsize; i++) {
        phc[i] = -1;
        mhc[i] = 0;
      }
      let j = 0;
      for (; j < vsize - 1; j++) {
        let mv = 0;
        let pv = -1;
        const start = j * 32;
        const end = Math.min(32, m) + start;
        for (let k = start; k < end; k++) {
          peq[b.charCodeAt(k)] |= 1 << k;
        }
        score = m;
        for (let i = 0; i < n; i++) {
          const eq = peq[a.charCodeAt(i)];
          const pb = (phc[(i / 32) | 0] >>> i) & 1;
          const mb = (mhc[(i / 32) | 0] >>> i) & 1;
          const xv = eq | mv;
          const xh = ((((eq | mb) & pv) + pv) ^ pv) | eq | mb;
          let ph = mv | ~(xh | pv);
          let mh = pv & xh;
          if ((ph >>> 31) ^ pb) {
            phc[(i / 32) | 0] ^= 1 << i;
          }
          if ((mh >>> 31) ^ mb) {
            mhc[(i / 32) | 0] ^= 1 << i;
          }
          ph = (ph << 1) | pb;
          mh = (mh << 1) | mb;
          pv = mh | ~(xv | ph);
          mv = ph & xv;
        }
        for (let k = start; k < end; k++) {
          peq[b.charCodeAt(k)] = 0;
        }
      }
      let mv = 0;
      let pv = -1;
      const start = j * 32;
      const end = Math.min(32, m - start) + start;
      for (let k = start; k < end; k++) {
        peq[b.charCodeAt(k)] |= 1 << k;
      }
      score = m;
      for (let i = 0; i < n; i++) {
        const eq = peq[a.charCodeAt(i)];
        const pb = (phc[(i / 32) | 0] >>> i) & 1;
        const mb = (mhc[(i / 32) | 0] >>> i) & 1;
        const xv = eq | mv;
        const xh = ((((eq | mb) & pv) + pv) ^ pv) | eq | mb;
        let ph = mv | ~(xh | pv);
        let mh = pv & xh;
        score += (ph >>> (m - 1)) & 1;
        score -= (mh >>> (m - 1)) & 1;
        if ((ph >>> 31) ^ pb) {
          phc[(i / 32) | 0] ^= 1 << i;
        }
        if ((mh >>> 31) ^ mb) {
          mhc[(i / 32) | 0] ^= 1 << i;
        }
        ph = (ph << 1) | pb;
        mh = (mh << 1) | mb;
        pv = mh | ~(xv | ph);
        mv = ph & xv;
      }
      for (let k = start; k < end; k++) {
        peq[b.charCodeAt(k)] = 0;
      }
      return score;
    };

    const distance = (a, b) => {
      if (a.length > b.length) {
        const tmp = b;
        b = a;
        a = tmp;
      }
      if (a.length === 0) {
        return b.length;
      }
      if (a.length <= 32) {
        return myers_32(a, b);
      }
      return myers_x(a, b);
    };

    const closest = (str, arr) => {
      let min_distance = Infinity;
      let min_index = 0;
      for (let i = 0; i < arr.length; i++) {
        const dist = distance(str, arr[i]);
        if (dist < min_distance) {
          min_distance = dist;
          min_index = i;
        }
      }
      return arr[min_index];
    };

    var fastestLevenshtein = {
      closest, distance
    };

    const getUsedThreshold = (password, entry, threshold) => {
      const isPasswordToShort = password.length <= entry.length;
      const isThresholdLongerThanPassword = password.length <= threshold;
      const shouldUsePasswordLength = isPasswordToShort || isThresholdLongerThanPassword; // if password is too small use the password length divided by 4 while the threshold needs to be at least 1

      return shouldUsePasswordLength ? Math.ceil(password.length / 4) : threshold;
    };

    const findLevenshteinDistance = (password, rankedDictionary, threshold) => {
      let foundDistance = 0;
      const found = Object.keys(rankedDictionary).find(entry => {
        const usedThreshold = getUsedThreshold(password, entry, threshold);
        const foundEntryDistance = fastestLevenshtein.distance(password, entry);
        const isInThreshold = foundEntryDistance <= usedThreshold;

        if (isInThreshold) {
          foundDistance = foundEntryDistance;
        }

        return isInThreshold;
      });

      if (found) {
        return {
          levenshteinDistance: foundDistance,
          levenshteinDistanceEntry: found
        };
      }

      return {};
    };

    var l33tTable = {
      a: ['4', '@'],
      b: ['8'],
      c: ['(', '{', '[', '<'],
      e: ['3'],
      g: ['6', '9'],
      i: ['1', '!', '|'],
      l: ['1', '|', '7'],
      o: ['0'],
      s: ['$', '5'],
      t: ['+', '7'],
      x: ['%'],
      z: ['2']
    };

    var translationKeys = {
      warnings: {
        straightRow: 'straightRow',
        keyPattern: 'keyPattern',
        simpleRepeat: 'simpleRepeat',
        extendedRepeat: 'extendedRepeat',
        sequences: 'sequences',
        recentYears: 'recentYears',
        dates: 'dates',
        topTen: 'topTen',
        topHundred: 'topHundred',
        common: 'common',
        similarToCommon: 'similarToCommon',
        wordByItself: 'wordByItself',
        namesByThemselves: 'namesByThemselves',
        commonNames: 'commonNames',
        userInputs: 'userInputs',
        pwned: 'pwned'
      },
      suggestions: {
        l33t: 'l33t',
        reverseWords: 'reverseWords',
        allUppercase: 'allUppercase',
        capitalization: 'capitalization',
        dates: 'dates',
        recentYears: 'recentYears',
        associatedYears: 'associatedYears',
        sequences: 'sequences',
        repeated: 'repeated',
        longerKeyboardPattern: 'longerKeyboardPattern',
        anotherWord: 'anotherWord',
        useWords: 'useWords',
        noNeed: 'noNeed',
        pwned: 'pwned'
      },
      timeEstimation: {
        ltSecond: 'ltSecond',
        second: 'second',
        seconds: 'seconds',
        minute: 'minute',
        minutes: 'minutes',
        hour: 'hour',
        hours: 'hours',
        day: 'day',
        days: 'days',
        month: 'month',
        months: 'months',
        year: 'year',
        years: 'years',
        centuries: 'centuries'
      }
    };

    class Options {
      constructor() {
        this.matchers = {};
        this.l33tTable = l33tTable;
        this.dictionary = {
          userInputs: []
        };
        this.rankedDictionaries = {};
        this.translations = translationKeys;
        this.graphs = {};
        this.availableGraphs = [];
        this.useLevenshteinDistance = false;
        this.levenshteinThreshold = 2;
        this.setRankedDictionaries();
      }

      setOptions(options = {}) {
        if (options.l33tTable) {
          this.l33tTable = options.l33tTable;
        }

        if (options.dictionary) {
          this.dictionary = options.dictionary;
          this.setRankedDictionaries();
        }

        if (options.translations) {
          this.setTranslations(options.translations);
        }

        if (options.graphs) {
          this.graphs = options.graphs;
        }

        if (options.useLevenshteinDistance !== undefined) {
          this.useLevenshteinDistance = options.useLevenshteinDistance;
        }

        if (options.levenshteinThreshold !== undefined) {
          this.levenshteinThreshold = options.levenshteinThreshold;
        }
      }

      setTranslations(translations) {
        if (this.checkCustomTranslations(translations)) {
          this.translations = translations;
        } else {
          throw new Error('Invalid translations object fallback to keys');
        }
      }

      checkCustomTranslations(translations) {
        let valid = true;
        Object.keys(translationKeys).forEach(type => {
          if (type in translations) {
            const translationType = type;
            Object.keys(translationKeys[translationType]).forEach(key => {
              if (!(key in translations[translationType])) {
                valid = false;
              }
            });
          } else {
            valid = false;
          }
        });
        return valid;
      }

      setRankedDictionaries() {
        const rankedDictionaries = {};
        Object.keys(this.dictionary).forEach(name => {
          rankedDictionaries[name] = this.getRankedDictionary(name);
        });
        this.rankedDictionaries = rankedDictionaries;
      }

      getRankedDictionary(name) {
        const list = this.dictionary[name];

        if (name === 'userInputs') {
          const sanitizedInputs = [];
          list.forEach(input => {
            const inputType = typeof input;

            if (inputType === 'string' || inputType === 'number' || inputType === 'boolean') {
              sanitizedInputs.push(input.toString().toLowerCase());
            }
          });
          return buildRankedDictionary(sanitizedInputs);
        }

        return buildRankedDictionary(list);
      }

      extendUserInputsDictionary(dictionary) {
        if (this.dictionary.userInputs) {
          this.dictionary.userInputs = [...this.dictionary.userInputs, ...dictionary];
        } else {
          this.dictionary.userInputs = dictionary;
        }

        this.rankedDictionaries.userInputs = this.getRankedDictionary('userInputs');
      }

      addMatcher(name, matcher) {
        if (this.matchers[name]) {
          console.info('Matcher already exists');
        } else {
          this.matchers[name] = matcher;
        }
      }

    }
    const zxcvbnOptions = new Options();

    /*
     * -------------------------------------------------------------------------------
     *  Dictionary reverse matching --------------------------------------------------
     * -------------------------------------------------------------------------------
     */
    class MatchL33t$1 {
      constructor(defaultMatch) {
        this.defaultMatch = defaultMatch;
      }

      match({
        password
      }) {
        const passwordReversed = password.split('').reverse().join('');
        return this.defaultMatch({
          password: passwordReversed
        }).map(match => ({ ...match,
          token: match.token.split('').reverse().join(''),
          reversed: true,
          // map coordinates back to original string
          i: password.length - 1 - match.j,
          j: password.length - 1 - match.i
        }));
      }

    }

    /*
     * -------------------------------------------------------------------------------
     *  Dictionary l33t matching -----------------------------------------------------
     * -------------------------------------------------------------------------------
     */

    class MatchL33t {
      constructor(defaultMatch) {
        this.defaultMatch = defaultMatch;
      }

      match({
        password
      }) {
        const matches = [];
        const enumeratedSubs = this.enumerateL33tSubs(this.relevantL33tSubtable(password, zxcvbnOptions.l33tTable));

        for (let i = 0; i < enumeratedSubs.length; i += 1) {
          const sub = enumeratedSubs[i]; // corner case: password has no relevant subs.

          if (empty(sub)) {
            break;
          }

          const subbedPassword = translate(password, sub);
          const matchedDictionary = this.defaultMatch({
            password: subbedPassword
          });
          matchedDictionary.forEach(match => {
            const token = password.slice(match.i, +match.j + 1 || 9e9); // only return the matches that contain an actual substitution

            if (token.toLowerCase() !== match.matchedWord) {
              // subset of mappings in sub that are in use for this match
              const matchSub = {};
              Object.keys(sub).forEach(subbedChr => {
                const chr = sub[subbedChr];

                if (token.indexOf(subbedChr) !== -1) {
                  matchSub[subbedChr] = chr;
                }
              });
              const subDisplay = Object.keys(matchSub).map(k => `${k} -> ${matchSub[k]}`).join(', ');
              matches.push({ ...match,
                l33t: true,
                token,
                sub: matchSub,
                subDisplay
              });
            }
          });
        } // filter single-character l33t matches to reduce noise.
        // otherwise '1' matches 'i', '4' matches 'a', both very common English words
        // with low dictionary rank.


        return matches.filter(match => match.token.length > 1);
      } // makes a pruned copy of l33t_table that only includes password's possible substitutions


      relevantL33tSubtable(password, table) {
        const passwordChars = {};
        const subTable = {};
        password.split('').forEach(char => {
          passwordChars[char] = true;
        });
        Object.keys(table).forEach(letter => {
          const subs = table[letter];
          const relevantSubs = subs.filter(sub => sub in passwordChars);

          if (relevantSubs.length > 0) {
            subTable[letter] = relevantSubs;
          }
        });
        return subTable;
      } // returns the list of possible 1337 replacement dictionaries for a given password


      enumerateL33tSubs(table) {
        const tableKeys = Object.keys(table);
        const subs = this.getSubs(tableKeys, [[]], table); // convert from assoc lists to dicts

        return subs.map(sub => {
          const subDict = {};
          sub.forEach(([l33tChr, chr]) => {
            subDict[l33tChr] = chr;
          });
          return subDict;
        });
      }

      getSubs(keys, subs, table) {
        if (!keys.length) {
          return subs;
        }

        const firstKey = keys[0];
        const restKeys = keys.slice(1);
        const nextSubs = [];
        table[firstKey].forEach(l33tChr => {
          subs.forEach(sub => {
            let dupL33tIndex = -1;

            for (let i = 0; i < sub.length; i += 1) {
              if (sub[i][0] === l33tChr) {
                dupL33tIndex = i;
                break;
              }
            }

            if (dupL33tIndex === -1) {
              const subExtension = sub.concat([[l33tChr, firstKey]]);
              nextSubs.push(subExtension);
            } else {
              const subAlternative = sub.slice(0);
              subAlternative.splice(dupL33tIndex, 1);
              subAlternative.push([l33tChr, firstKey]);
              nextSubs.push(sub);
              nextSubs.push(subAlternative);
            }
          });
        });
        const newSubs = this.dedup(nextSubs);

        if (restKeys.length) {
          return this.getSubs(restKeys, newSubs, table);
        }

        return newSubs;
      }

      dedup(subs) {
        const deduped = [];
        const members = {};
        subs.forEach(sub => {
          const assoc = sub.map((k, index) => [k, index]);
          assoc.sort();
          const label = assoc.map(([k, v]) => `${k},${v}`).join('-');

          if (!(label in members)) {
            members[label] = true;
            deduped.push(sub);
          }
        });
        return deduped;
      }

    }

    class MatchDictionary {
      constructor() {
        this.l33t = new MatchL33t(this.defaultMatch);
        this.reverse = new MatchL33t$1(this.defaultMatch);
      }

      match({
        password
      }) {
        const matches = [...this.defaultMatch({
          password
        }), ...this.reverse.match({
          password
        }), ...this.l33t.match({
          password
        })];
        return sorted(matches);
      }

      defaultMatch({
        password
      }) {
        const matches = [];
        const passwordLength = password.length;
        const passwordLower = password.toLowerCase(); // eslint-disable-next-line complexity

        Object.keys(zxcvbnOptions.rankedDictionaries).forEach(dictionaryName => {
          const rankedDict = zxcvbnOptions.rankedDictionaries[dictionaryName];

          for (let i = 0; i < passwordLength; i += 1) {
            for (let j = i; j < passwordLength; j += 1) {
              const usedPassword = passwordLower.slice(i, +j + 1 || 9e9);
              const isInDictionary = (usedPassword in rankedDict);
              let foundLevenshteinDistance = {}; // only use levenshtein distance on full password to minimize the performance drop
              // and because otherwise there would be to many false positives

              const isFullPassword = i === 0 && j === passwordLength - 1;

              if (zxcvbnOptions.useLevenshteinDistance && isFullPassword && !isInDictionary) {
                foundLevenshteinDistance = findLevenshteinDistance(usedPassword, rankedDict, zxcvbnOptions.levenshteinThreshold);
              }

              const isLevenshteinMatch = Object.keys(foundLevenshteinDistance).length !== 0;

              if (isInDictionary || isLevenshteinMatch) {
                const usedRankPassword = isLevenshteinMatch ? foundLevenshteinDistance.levenshteinDistanceEntry : usedPassword;
                const rank = rankedDict[usedRankPassword];
                matches.push({
                  pattern: 'dictionary',
                  i,
                  j,
                  token: password.slice(i, +j + 1 || 9e9),
                  matchedWord: usedPassword,
                  rank,
                  dictionaryName: dictionaryName,
                  reversed: false,
                  l33t: false,
                  ...foundLevenshteinDistance
                });
              }
            }
          }
        });
        return matches;
      }

    }

    /*
     * -------------------------------------------------------------------------------
     *  regex matching ---------------------------------------------------------------
     * -------------------------------------------------------------------------------
     */

    class MatchRegex {
      match({
        password,
        regexes = REGEXEN
      }) {
        const matches = [];
        Object.keys(regexes).forEach(name => {
          const regex = regexes[name];
          regex.lastIndex = 0; // keeps regexMatch stateless

          const regexMatch = regex.exec(password);

          if (regexMatch) {
            const token = regexMatch[0];
            matches.push({
              pattern: 'regex',
              token,
              i: regexMatch.index,
              j: regexMatch.index + regexMatch[0].length - 1,
              regexName: name,
              regexMatch
            });
          }
        });
        return sorted(matches);
      }

    }

    var utils = {
      // binomial coefficients
      // src: http://blog.plover.com/math/choose.html
      nCk(n, k) {
        let count = n;

        if (k > count) {
          return 0;
        }

        if (k === 0) {
          return 1;
        }

        let coEff = 1;

        for (let i = 1; i <= k; i += 1) {
          coEff *= count;
          coEff /= i;
          count -= 1;
        }

        return coEff;
      },

      log10(n) {
        return Math.log(n) / Math.log(10); // IE doesn't support Math.log10 :(
      },

      log2(n) {
        return Math.log(n) / Math.log(2);
      },

      factorial(num) {
        let rval = 1;

        for (let i = 2; i <= num; i += 1) rval *= i;

        return rval;
      }

    };

    var bruteforceMatcher$1 = (({
      token
    }) => {
      let guesses = BRUTEFORCE_CARDINALITY ** token.length;

      if (guesses === Number.POSITIVE_INFINITY) {
        guesses = Number.MAX_VALUE;
      }

      let minGuesses; // small detail: make bruteforce matches at minimum one guess bigger than smallest allowed
      // submatch guesses, such that non-bruteforce submatches over the same [i..j] take precedence.

      if (token.length === 1) {
        minGuesses = MIN_SUBMATCH_GUESSES_SINGLE_CHAR + 1;
      } else {
        minGuesses = MIN_SUBMATCH_GUESSES_MULTI_CHAR + 1;
      }

      return Math.max(guesses, minGuesses);
    });

    var dateMatcher$1 = (({
      year,
      separator
    }) => {
      // base guesses: (year distance from REFERENCE_YEAR) * num_days * num_years
      const yearSpace = Math.max(Math.abs(year - REFERENCE_YEAR), MIN_YEAR_SPACE);
      let guesses = yearSpace * 365; // add factor of 4 for separator selection (one of ~4 choices)

      if (separator) {
        guesses *= 4;
      }

      return guesses;
    });

    const getVariations = cleanedWord => {
      const wordArray = cleanedWord.split('');
      const upperCaseCount = wordArray.filter(char => char.match(ONE_UPPER)).length;
      const lowerCaseCount = wordArray.filter(char => char.match(ONE_LOWER)).length;
      let variations = 0;
      const variationLength = Math.min(upperCaseCount, lowerCaseCount);

      for (let i = 1; i <= variationLength; i += 1) {
        variations += utils.nCk(upperCaseCount + lowerCaseCount, i);
      }

      return variations;
    };

    var uppercaseVariant = (word => {
      // clean words of non alpha characters to remove the reward effekt to capitalize the first letter https://github.com/dropbox/zxcvbn/issues/232
      const cleanedWord = word.replace(ALPHA_INVERTED, '');

      if (cleanedWord.match(ALL_LOWER_INVERTED) || cleanedWord.toLowerCase() === cleanedWord) {
        return 1;
      } // a capitalized word is the most common capitalization scheme,
      // so it only doubles the search space (uncapitalized + capitalized).
      // all caps and end-capitalized are common enough too, underestimate as 2x factor to be safe.


      const commonCases = [START_UPPER, END_UPPER, ALL_UPPER_INVERTED];
      const commonCasesLength = commonCases.length;

      for (let i = 0; i < commonCasesLength; i += 1) {
        const regex = commonCases[i];

        if (cleanedWord.match(regex)) {
          return 2;
        }
      } // otherwise calculate the number of ways to capitalize U+L uppercase+lowercase letters
      // with U uppercase letters or less. or, if there's more uppercase than lower (for eg. PASSwORD),
      // the number of ways to lowercase U+L letters with L lowercase letters or less.


      return getVariations(cleanedWord);
    });

    const getCounts = ({
      subs,
      subbed,
      token
    }) => {
      const unsubbed = subs[subbed]; // lower-case match.token before calculating: capitalization shouldn't affect l33t calc.

      const chrs = token.toLowerCase().split(''); // num of subbed chars

      const subbedCount = chrs.filter(char => char === subbed).length; // num of unsubbed chars

      const unsubbedCount = chrs.filter(char => char === unsubbed).length;
      return {
        subbedCount,
        unsubbedCount
      };
    };

    var l33tVariant = (({
      l33t,
      sub,
      token
    }) => {
      if (!l33t) {
        return 1;
      }

      let variations = 1;
      const subs = sub;
      Object.keys(subs).forEach(subbed => {
        const {
          subbedCount,
          unsubbedCount
        } = getCounts({
          subs,
          subbed,
          token
        });

        if (subbedCount === 0 || unsubbedCount === 0) {
          // for this sub, password is either fully subbed (444) or fully unsubbed (aaa)
          // treat that as doubling the space (attacker needs to try fully subbed chars in addition to
          // unsubbed.)
          variations *= 2;
        } else {
          // this case is similar to capitalization:
          // with aa44a, U = 3, S = 2, attacker needs to try unsubbed + one sub + two subs
          const p = Math.min(unsubbedCount, subbedCount);
          let possibilities = 0;

          for (let i = 1; i <= p; i += 1) {
            possibilities += utils.nCk(unsubbedCount + subbedCount, i);
          }

          variations *= possibilities;
        }
      });
      return variations;
    });

    var dictionaryMatcher$1 = (({
      rank,
      reversed,
      l33t,
      sub,
      token
    }) => {
      const baseGuesses = rank; // keep these as properties for display purposes

      const uppercaseVariations = uppercaseVariant(token);
      const l33tVariations = l33tVariant({
        l33t,
        sub,
        token
      });
      const reversedVariations = reversed && 2 || 1;
      const calculation = baseGuesses * uppercaseVariations * l33tVariations * reversedVariations;
      return {
        baseGuesses,
        uppercaseVariations,
        l33tVariations,
        calculation
      };
    });

    var regexMatcher$1 = (({
      regexName,
      regexMatch,
      token
    }) => {
      const charClassBases = {
        alphaLower: 26,
        alphaUpper: 26,
        alpha: 52,
        alphanumeric: 62,
        digits: 10,
        symbols: 33
      };

      if (regexName in charClassBases) {
        return charClassBases[regexName] ** token.length;
      } // TODO add more regex types for example special dates like 09.11
      // eslint-disable-next-line default-case


      switch (regexName) {
        case 'recentYear':
          // conservative estimate of year space: num years from REFERENCE_YEAR.
          // if year is close to REFERENCE_YEAR, estimate a year space of MIN_YEAR_SPACE.
          return Math.max(Math.abs(parseInt(regexMatch[0], 10) - REFERENCE_YEAR), MIN_YEAR_SPACE);
      }

      return 0;
    });

    var repeatMatcher$1 = (({
      baseGuesses,
      repeatCount
    }) => baseGuesses * repeatCount);

    var sequenceMatcher$1 = (({
      token,
      ascending
    }) => {
      const firstChr = token.charAt(0);
      let baseGuesses = 0;
      const startingPoints = ['a', 'A', 'z', 'Z', '0', '1', '9']; // lower guesses for obvious starting points

      if (startingPoints.includes(firstChr)) {
        baseGuesses = 4;
      } else if (firstChr.match(/\d/)) {
        baseGuesses = 10; // digits
      } else {
        // could give a higher base for uppercase,
        // assigning 26 to both upper and lower sequences is more conservative.
        baseGuesses = 26;
      } // need to try a descending sequence in addition to every ascending sequence ->
      // 2x guesses


      if (!ascending) {
        baseGuesses *= 2;
      }

      return baseGuesses * token.length;
    });

    const calcAverageDegree = graph => {
      let average = 0;
      Object.keys(graph).forEach(key => {
        const neighbors = graph[key];
        average += neighbors.filter(entry => !!entry).length;
      });
      average /= Object.entries(graph).length;
      return average;
    };

    const estimatePossiblePatterns = ({
      token,
      graph,
      turns
    }) => {
      const startingPosition = Object.keys(zxcvbnOptions.graphs[graph]).length;
      const averageDegree = calcAverageDegree(zxcvbnOptions.graphs[graph]);
      let guesses = 0;
      const tokenLength = token.length; // # estimate the number of possible patterns w/ tokenLength or less with turns or less.

      for (let i = 2; i <= tokenLength; i += 1) {
        const possibleTurns = Math.min(turns, i - 1);

        for (let j = 1; j <= possibleTurns; j += 1) {
          guesses += utils.nCk(i - 1, j - 1) * startingPosition * averageDegree ** j;
        }
      }

      return guesses;
    };

    var spatialMatcher$1 = (({
      graph,
      token,
      shiftedCount,
      turns
    }) => {
      let guesses = estimatePossiblePatterns({
        token,
        graph,
        turns
      }); // add extra guesses for shifted keys. (% instead of 5, A instead of a.)
      // math is similar to extra guesses of l33t substitutions in dictionary matches.

      if (shiftedCount) {
        const unShiftedCount = token.length - shiftedCount;

        if (shiftedCount === 0 || unShiftedCount === 0) {
          guesses *= 2;
        } else {
          let shiftedVariations = 0;

          for (let i = 1; i <= Math.min(shiftedCount, unShiftedCount); i += 1) {
            shiftedVariations += utils.nCk(shiftedCount + unShiftedCount, i);
          }

          guesses *= shiftedVariations;
        }
      }

      return Math.round(guesses);
    });

    const getMinGuesses = (match, password) => {
      let minGuesses = 1;

      if (match.token.length < password.length) {
        if (match.token.length === 1) {
          minGuesses = MIN_SUBMATCH_GUESSES_SINGLE_CHAR;
        } else {
          minGuesses = MIN_SUBMATCH_GUESSES_MULTI_CHAR;
        }
      }

      return minGuesses;
    };

    const matchers = {
      bruteforce: bruteforceMatcher$1,
      date: dateMatcher$1,
      dictionary: dictionaryMatcher$1,
      regex: regexMatcher$1,
      repeat: repeatMatcher$1,
      sequence: sequenceMatcher$1,
      spatial: spatialMatcher$1
    };

    const getScoring = (name, match) => {
      if (matchers[name]) {
        return matchers[name](match);
      }

      if (zxcvbnOptions.matchers[name] && 'scoring' in zxcvbnOptions.matchers[name]) {
        return zxcvbnOptions.matchers[name].scoring(match);
      }

      return 0;
    }; // ------------------------------------------------------------------------------
    // guess estimation -- one function per match pattern ---------------------------
    // ------------------------------------------------------------------------------


    var estimateGuesses = ((match, password) => {
      const extraData = {}; // a match's guess estimate doesn't change. cache it.

      if ('guesses' in match && match.guesses != null) {
        return match;
      }

      const minGuesses = getMinGuesses(match, password);
      const estimationResult = getScoring(match.pattern, match);
      let guesses = 0;

      if (typeof estimationResult === 'number') {
        guesses = estimationResult;
      } else if (match.pattern === 'dictionary') {
        guesses = estimationResult.calculation;
        extraData.baseGuesses = estimationResult.baseGuesses;
        extraData.uppercaseVariations = estimationResult.uppercaseVariations;
        extraData.l33tVariations = estimationResult.l33tVariations;
      }

      const matchGuesses = Math.max(guesses, minGuesses);
      return { ...match,
        ...extraData,
        guesses: matchGuesses,
        guessesLog10: utils.log10(matchGuesses)
      };
    });

    const scoringHelper = {
      password: '',
      optimal: {},
      excludeAdditive: false,

      fillArray(size, valueType) {
        const result = [];

        for (let i = 0; i < size; i += 1) {
          let value = [];

          if (valueType === 'object') {
            value = {};
          }

          result.push(value);
        }

        return result;
      },

      // helper: make bruteforce match objects spanning i to j, inclusive.
      makeBruteforceMatch(i, j) {
        return {
          pattern: 'bruteforce',
          token: this.password.slice(i, +j + 1 || 9e9),
          i,
          j
        };
      },

      // helper: considers whether a length-sequenceLength
      // sequence ending at match m is better (fewer guesses)
      // than previously encountered sequences, updating state if so.
      update(match, sequenceLength) {
        const k = match.j;
        const estimatedMatch = estimateGuesses(match, this.password);
        let pi = estimatedMatch.guesses;

        if (sequenceLength > 1) {
          // we're considering a length-sequenceLength sequence ending with match m:
          // obtain the product term in the minimization function by multiplying m's guesses
          // by the product of the length-(sequenceLength-1)
          // sequence ending just before m, at m.i - 1.
          pi *= this.optimal.pi[estimatedMatch.i - 1][sequenceLength - 1];
        } // calculate the minimization func


        let g = utils.factorial(sequenceLength) * pi;

        if (!this.excludeAdditive) {
          g += MIN_GUESSES_BEFORE_GROWING_SEQUENCE ** (sequenceLength - 1);
        } // update state if new best.
        // first see if any competing sequences covering this prefix,
        // with sequenceLength or fewer matches,
        // fare better than this sequence. if so, skip it and return.


        let shouldSkip = false;
        Object.keys(this.optimal.g[k]).forEach(competingPatternLength => {
          const competingMetricMatch = this.optimal.g[k][competingPatternLength];

          if (parseInt(competingPatternLength, 10) <= sequenceLength) {
            if (competingMetricMatch <= g) {
              shouldSkip = true;
            }
          }
        });

        if (!shouldSkip) {
          // this sequence might be part of the final optimal sequence.
          this.optimal.g[k][sequenceLength] = g;
          this.optimal.m[k][sequenceLength] = estimatedMatch;
          this.optimal.pi[k][sequenceLength] = pi;
        }
      },

      // helper: evaluate bruteforce matches ending at passwordCharIndex.
      bruteforceUpdate(passwordCharIndex) {
        // see if a single bruteforce match spanning the passwordCharIndex-prefix is optimal.
        let match = this.makeBruteforceMatch(0, passwordCharIndex);
        this.update(match, 1);

        for (let i = 1; i <= passwordCharIndex; i += 1) {
          // generate passwordCharIndex bruteforce matches, spanning from (i=1, j=passwordCharIndex) up to (i=passwordCharIndex, j=passwordCharIndex).
          // see if adding these new matches to any of the sequences in optimal[i-1]
          // leads to new bests.
          match = this.makeBruteforceMatch(i, passwordCharIndex);
          const tmp = this.optimal.m[i - 1]; // eslint-disable-next-line no-loop-func

          Object.keys(tmp).forEach(sequenceLength => {
            const lastMatch = tmp[sequenceLength]; // corner: an optimal sequence will never have two adjacent bruteforce matches.
            // it is strictly better to have a single bruteforce match spanning the same region:
            // same contribution to the guess product with a lower length.
            // --> safe to skip those cases.

            if (lastMatch.pattern !== 'bruteforce') {
              // try adding m to this length-sequenceLength sequence.
              this.update(match, parseInt(sequenceLength, 10) + 1);
            }
          });
        }
      },

      // helper: step backwards through optimal.m starting at the end,
      // constructing the final optimal match sequence.
      unwind(passwordLength) {
        const optimalMatchSequence = [];
        let k = passwordLength - 1; // find the final best sequence length and score

        let sequenceLength = 0; // eslint-disable-next-line no-loss-of-precision

        let g = 2e308;
        const temp = this.optimal.g[k]; // safety check for empty passwords

        if (temp) {
          Object.keys(temp).forEach(candidateSequenceLength => {
            const candidateMetricMatch = temp[candidateSequenceLength];

            if (candidateMetricMatch < g) {
              sequenceLength = parseInt(candidateSequenceLength, 10);
              g = candidateMetricMatch;
            }
          });
        }

        while (k >= 0) {
          const match = this.optimal.m[k][sequenceLength];
          optimalMatchSequence.unshift(match);
          k = match.i - 1;
          sequenceLength -= 1;
        }

        return optimalMatchSequence;
      }

    };
    var scoring = {
      // ------------------------------------------------------------------------------
      // search --- most guessable match sequence -------------------------------------
      // ------------------------------------------------------------------------------
      //
      // takes a sequence of overlapping matches, returns the non-overlapping sequence with
      // minimum guesses. the following is a O(l_max * (n + m)) dynamic programming algorithm
      // for a length-n password with m candidate matches. l_max is the maximum optimal
      // sequence length spanning each prefix of the password. In practice it rarely exceeds 5 and the
      // search terminates rapidly.
      //
      // the optimal "minimum guesses" sequence is here defined to be the sequence that
      // minimizes the following function:
      //
      //    g = sequenceLength! * Product(m.guesses for m in sequence) + D^(sequenceLength - 1)
      //
      // where sequenceLength is the length of the sequence.
      //
      // the factorial term is the number of ways to order sequenceLength patterns.
      //
      // the D^(sequenceLength-1) term is another length penalty, roughly capturing the idea that an
      // attacker will try lower-length sequences first before trying length-sequenceLength sequences.
      //
      // for example, consider a sequence that is date-repeat-dictionary.
      //  - an attacker would need to try other date-repeat-dictionary combinations,
      //    hence the product term.
      //  - an attacker would need to try repeat-date-dictionary, dictionary-repeat-date,
      //    ..., hence the factorial term.
      //  - an attacker would also likely try length-1 (dictionary) and length-2 (dictionary-date)
      //    sequences before length-3. assuming at minimum D guesses per pattern type,
      //    D^(sequenceLength-1) approximates Sum(D^i for i in [1..sequenceLength-1]
      //
      // ------------------------------------------------------------------------------
      mostGuessableMatchSequence(password, matches, excludeAdditive = false) {
        scoringHelper.password = password;
        scoringHelper.excludeAdditive = excludeAdditive;
        const passwordLength = password.length; // partition matches into sublists according to ending index j

        let matchesByCoordinateJ = scoringHelper.fillArray(passwordLength, 'array');
        matches.forEach(match => {
          matchesByCoordinateJ[match.j].push(match);
        }); // small detail: for deterministic output, sort each sublist by i.

        matchesByCoordinateJ = matchesByCoordinateJ.map(match => match.sort((m1, m2) => m1.i - m2.i));
        scoringHelper.optimal = {
          // optimal.m[k][sequenceLength] holds final match in the best length-sequenceLength
          // match sequence covering the
          // password prefix up to k, inclusive.
          // if there is no length-sequenceLength sequence that scores better (fewer guesses) than
          // a shorter match sequence spanning the same prefix,
          // optimal.m[k][sequenceLength] is undefined.
          m: scoringHelper.fillArray(passwordLength, 'object'),
          // same structure as optimal.m -- holds the product term Prod(m.guesses for m in sequence).
          // optimal.pi allows for fast (non-looping) updates to the minimization function.
          pi: scoringHelper.fillArray(passwordLength, 'object'),
          // same structure as optimal.m -- holds the overall metric.
          g: scoringHelper.fillArray(passwordLength, 'object')
        };

        for (let k = 0; k < passwordLength; k += 1) {
          matchesByCoordinateJ[k].forEach(match => {
            if (match.i > 0) {
              Object.keys(scoringHelper.optimal.m[match.i - 1]).forEach(sequenceLength => {
                scoringHelper.update(match, parseInt(sequenceLength, 10) + 1);
              });
            } else {
              scoringHelper.update(match, 1);
            }
          });
          scoringHelper.bruteforceUpdate(k);
        }

        const optimalMatchSequence = scoringHelper.unwind(passwordLength);
        const optimalSequenceLength = optimalMatchSequence.length;
        const guesses = this.getGuesses(password, optimalSequenceLength);
        return {
          password,
          guesses,
          guessesLog10: utils.log10(guesses),
          sequence: optimalMatchSequence
        };
      },

      getGuesses(password, optimalSequenceLength) {
        const passwordLength = password.length;
        let guesses = 0;

        if (password.length === 0) {
          guesses = 1;
        } else {
          guesses = scoringHelper.optimal.g[passwordLength - 1][optimalSequenceLength];
        }

        return guesses;
      }

    };

    /*
     *-------------------------------------------------------------------------------
     * repeats (aaa, abcabcabc) ------------------------------
     *-------------------------------------------------------------------------------
     */

    class MatchRepeat {
      // eslint-disable-next-line max-statements
      match({
        password,
        omniMatch
      }) {
        const matches = [];
        let lastIndex = 0;

        while (lastIndex < password.length) {
          const greedyMatch = this.getGreedyMatch(password, lastIndex);
          const lazyMatch = this.getLazyMatch(password, lastIndex);

          if (greedyMatch == null) {
            break;
          }

          const {
            match,
            baseToken
          } = this.setMatchToken(greedyMatch, lazyMatch);

          if (match) {
            const j = match.index + match[0].length - 1;
            const baseGuesses = this.getBaseGuesses(baseToken, omniMatch);
            matches.push(this.normalizeMatch(baseToken, j, match, baseGuesses));
            lastIndex = j + 1;
          }
        }

        const hasPromises = matches.some(match => {
          return match instanceof Promise;
        });

        if (hasPromises) {
          return Promise.all(matches);
        }

        return matches;
      } // eslint-disable-next-line max-params


      normalizeMatch(baseToken, j, match, baseGuesses) {
        const baseMatch = {
          pattern: 'repeat',
          i: match.index,
          j,
          token: match[0],
          baseToken,
          baseGuesses: 0,
          repeatCount: match[0].length / baseToken.length
        };

        if (baseGuesses instanceof Promise) {
          return baseGuesses.then(resolvedBaseGuesses => {
            return { ...baseMatch,
              baseGuesses: resolvedBaseGuesses
            };
          });
        }

        return { ...baseMatch,
          baseGuesses
        };
      }

      getGreedyMatch(password, lastIndex) {
        const greedy = /(.+)\1+/g;
        greedy.lastIndex = lastIndex;
        return greedy.exec(password);
      }

      getLazyMatch(password, lastIndex) {
        const lazy = /(.+?)\1+/g;
        lazy.lastIndex = lastIndex;
        return lazy.exec(password);
      }

      setMatchToken(greedyMatch, lazyMatch) {
        const lazyAnchored = /^(.+?)\1+$/;
        let match;
        let baseToken = '';

        if (lazyMatch && greedyMatch[0].length > lazyMatch[0].length) {
          // greedy beats lazy for 'aabaab'
          // greedy: [aabaab, aab]
          // lazy:   [aa,     a]
          match = greedyMatch; // greedy's repeated string might itself be repeated, eg.
          // aabaab in aabaabaabaab.
          // run an anchored lazy match on greedy's repeated string
          // to find the shortest repeated string

          const temp = lazyAnchored.exec(match[0]);

          if (temp) {
            baseToken = temp[1];
          }
        } else {
          // lazy beats greedy for 'aaaaa'
          // greedy: [aaaa,  aa]
          // lazy:   [aaaaa, a]
          match = lazyMatch;

          if (match) {
            baseToken = match[1];
          }
        }

        return {
          match,
          baseToken
        };
      }

      getBaseGuesses(baseToken, omniMatch) {
        const matches = omniMatch.match(baseToken);

        if (matches instanceof Promise) {
          return matches.then(resolvedMatches => {
            const baseAnalysis = scoring.mostGuessableMatchSequence(baseToken, resolvedMatches);
            return baseAnalysis.guesses;
          });
        }

        const baseAnalysis = scoring.mostGuessableMatchSequence(baseToken, matches);
        return baseAnalysis.guesses;
      }

    }

    /*
     *-------------------------------------------------------------------------------
     * sequences (abcdef) ------------------------------
     *-------------------------------------------------------------------------------
     */

    class MatchSequence {
      constructor() {
        this.MAX_DELTA = 5;
      } // eslint-disable-next-line max-statements


      match({
        password
      }) {
        /*
         * Identifies sequences by looking for repeated differences in unicode codepoint.
         * this allows skipping, such as 9753, and also matches some extended unicode sequences
         * such as Greek and Cyrillic alphabets.
         *
         * for example, consider the input 'abcdb975zy'
         *
         * password: a   b   c   d   b    9   7   5   z   y
         * index:    0   1   2   3   4    5   6   7   8   9
         * delta:      1   1   1  -2  -41  -2  -2  69   1
         *
         * expected result:
         * [(i, j, delta), ...] = [(0, 3, 1), (5, 7, -2), (8, 9, 1)]
         */
        const result = [];

        if (password.length === 1) {
          return [];
        }

        let i = 0;
        let lastDelta = null;
        const passwordLength = password.length;

        for (let k = 1; k < passwordLength; k += 1) {
          const delta = password.charCodeAt(k) - password.charCodeAt(k - 1);

          if (lastDelta == null) {
            lastDelta = delta;
          }

          if (delta !== lastDelta) {
            const j = k - 1;
            this.update({
              i,
              j,
              delta: lastDelta,
              password,
              result
            });
            i = j;
            lastDelta = delta;
          }
        }

        this.update({
          i,
          j: passwordLength - 1,
          delta: lastDelta,
          password,
          result
        });
        return result;
      }

      update({
        i,
        j,
        delta,
        password,
        result
      }) {
        if (j - i > 1 || Math.abs(delta) === 1) {
          const absoluteDelta = Math.abs(delta);

          if (absoluteDelta > 0 && absoluteDelta <= this.MAX_DELTA) {
            const token = password.slice(i, +j + 1 || 9e9);
            const {
              sequenceName,
              sequenceSpace
            } = this.getSequence(token);
            return result.push({
              pattern: 'sequence',
              i,
              j,
              token: password.slice(i, +j + 1 || 9e9),
              sequenceName,
              sequenceSpace,
              ascending: delta > 0
            });
          }
        }

        return null;
      }

      getSequence(token) {
        // TODO conservatively stick with roman alphabet size.
        //  (this could be improved)
        let sequenceName = 'unicode';
        let sequenceSpace = 26;

        if (ALL_LOWER.test(token)) {
          sequenceName = 'lower';
          sequenceSpace = 26;
        } else if (ALL_UPPER.test(token)) {
          sequenceName = 'upper';
          sequenceSpace = 26;
        } else if (ALL_DIGIT.test(token)) {
          sequenceName = 'digits';
          sequenceSpace = 10;
        }

        return {
          sequenceName,
          sequenceSpace
        };
      }

    }

    /*
     * ------------------------------------------------------------------------------
     * spatial match (qwerty/dvorak/keypad and so on) -----------------------------------------
     * ------------------------------------------------------------------------------
     */

    class MatchSpatial {
      constructor() {
        this.SHIFTED_RX = /[~!@#$%^&*()_+QWERTYUIOP{}|ASDFGHJKL:"ZXCVBNM<>?]/;
      }

      match({
        password
      }) {
        const matches = [];
        Object.keys(zxcvbnOptions.graphs).forEach(graphName => {
          const graph = zxcvbnOptions.graphs[graphName];
          extend(matches, this.helper(password, graph, graphName));
        });
        return sorted(matches);
      }

      checkIfShifted(graphName, password, index) {
        if (!graphName.includes('keypad') && // initial character is shifted
        this.SHIFTED_RX.test(password.charAt(index))) {
          return 1;
        }

        return 0;
      } // eslint-disable-next-line complexity, max-statements


      helper(password, graph, graphName) {
        let shiftedCount;
        const matches = [];
        let i = 0;
        const passwordLength = password.length;

        while (i < passwordLength - 1) {
          let j = i + 1;
          let lastDirection = 0;
          let turns = 0;
          shiftedCount = this.checkIfShifted(graphName, password, i); // eslint-disable-next-line no-constant-condition

          while (true) {
            const prevChar = password.charAt(j - 1);
            const adjacents = graph[prevChar] || [];
            let found = false;
            let foundDirection = -1;
            let curDirection = -1; // consider growing pattern by one character if j hasn't gone over the edge.

            if (j < passwordLength) {
              const curChar = password.charAt(j);
              const adjacentsLength = adjacents.length;

              for (let k = 0; k < adjacentsLength; k += 1) {
                const adjacent = adjacents[k];
                curDirection += 1; // eslint-disable-next-line max-depth

                if (adjacent) {
                  const adjacentIndex = adjacent.indexOf(curChar); // eslint-disable-next-line max-depth

                  if (adjacentIndex !== -1) {
                    found = true;
                    foundDirection = curDirection; // eslint-disable-next-line max-depth

                    if (adjacentIndex === 1) {
                      // # index 1 in the adjacency means the key is shifted,
                      // # 0 means unshifted: A vs a, % vs 5, etc.
                      // # for example, 'q' is adjacent to the entry '2@'.
                      // # @ is shifted w/ index 1, 2 is unshifted.
                      shiftedCount += 1;
                    } // eslint-disable-next-line max-depth


                    if (lastDirection !== foundDirection) {
                      // # adding a turn is correct even in the initial
                      // case when last_direction is null:
                      // # every spatial pattern starts with a turn.
                      turns += 1;
                      lastDirection = foundDirection;
                    }

                    break;
                  }
                }
              }
            } // if the current pattern continued, extend j and try to grow again


            if (found) {
              j += 1; // otherwise push the pattern discovered so far, if any...
            } else {
              // don't consider length 1 or 2 chains.
              if (j - i > 2) {
                matches.push({
                  pattern: 'spatial',
                  i,
                  j: j - 1,
                  token: password.slice(i, j),
                  graph: graphName,
                  turns,
                  shiftedCount
                });
              } // ...and then start a new search for the rest of the password.


              i = j;
              break;
            }
          }
        }

        return matches;
      }

    }

    class Matching {
      constructor() {
        this.matchers = {
          date: MatchDate,
          dictionary: MatchDictionary,
          regex: MatchRegex,
          // @ts-ignore => TODO resolve this type issue. This is because it is possible to be async
          repeat: MatchRepeat,
          sequence: MatchSequence,
          spatial: MatchSpatial
        };
      }

      match(password) {
        const matches = [];
        const promises = [];
        const matchers = [...Object.keys(this.matchers), ...Object.keys(zxcvbnOptions.matchers)];
        matchers.forEach(key => {
          if (!this.matchers[key] && !zxcvbnOptions.matchers[key]) {
            return;
          }

          const Matcher = this.matchers[key] ? this.matchers[key] : zxcvbnOptions.matchers[key].Matching;
          const usedMatcher = new Matcher();
          const result = usedMatcher.match({
            password,
            omniMatch: this
          });

          if (result instanceof Promise) {
            result.then(response => {
              extend(matches, response);
            });
            promises.push(result);
          } else {
            extend(matches, result);
          }
        });

        if (promises.length > 0) {
          return new Promise(resolve => {
            Promise.all(promises).then(() => {
              resolve(sorted(matches));
            });
          });
        }

        return sorted(matches);
      }

    }

    const SECOND = 1;
    const MINUTE = SECOND * 60;
    const HOUR = MINUTE * 60;
    const DAY = HOUR * 24;
    const MONTH = DAY * 31;
    const YEAR = MONTH * 12;
    const CENTURY = YEAR * 100;
    const times = {
      second: SECOND,
      minute: MINUTE,
      hour: HOUR,
      day: DAY,
      month: MONTH,
      year: YEAR,
      century: CENTURY
    };
    /*
     * -------------------------------------------------------------------------------
     *  Estimates time for an attacker ---------------------------------------------------------------
     * -------------------------------------------------------------------------------
     */

    class TimeEstimates {
      translate(displayStr, value) {
        let key = displayStr;

        if (value !== undefined && value !== 1) {
          key += 's';
        }

        const {
          timeEstimation
        } = zxcvbnOptions.translations;
        return timeEstimation[key].replace('{base}', `${value}`);
      }

      estimateAttackTimes(guesses) {
        const crackTimesSeconds = {
          onlineThrottling100PerHour: guesses / (100 / 3600),
          onlineNoThrottling10PerSecond: guesses / 10,
          offlineSlowHashing1e4PerSecond: guesses / 1e4,
          offlineFastHashing1e10PerSecond: guesses / 1e10
        };
        const crackTimesDisplay = {
          onlineThrottling100PerHour: '',
          onlineNoThrottling10PerSecond: '',
          offlineSlowHashing1e4PerSecond: '',
          offlineFastHashing1e10PerSecond: ''
        };
        Object.keys(crackTimesSeconds).forEach(scenario => {
          const seconds = crackTimesSeconds[scenario];
          crackTimesDisplay[scenario] = this.displayTime(seconds);
        });
        return {
          crackTimesSeconds,
          crackTimesDisplay,
          score: this.guessesToScore(guesses)
        };
      }

      guessesToScore(guesses) {
        const DELTA = 5;

        if (guesses < 1e3 + DELTA) {
          // risky password: "too guessable"
          return 0;
        }

        if (guesses < 1e6 + DELTA) {
          // modest protection from throttled online attacks: "very guessable"
          return 1;
        }

        if (guesses < 1e8 + DELTA) {
          // modest protection from unthrottled online attacks: "somewhat guessable"
          return 2;
        }

        if (guesses < 1e10 + DELTA) {
          // modest protection from offline attacks: "safely unguessable"
          // assuming a salted, slow hash function like bcrypt, scrypt, PBKDF2, argon, etc
          return 3;
        } // strong protection from offline attacks under same scenario: "very unguessable"


        return 4;
      }

      displayTime(seconds) {
        let displayStr = 'centuries';
        let base;
        const timeKeys = Object.keys(times);
        const foundIndex = timeKeys.findIndex(time => seconds < times[time]);

        if (foundIndex > -1) {
          displayStr = timeKeys[foundIndex - 1];

          if (foundIndex !== 0) {
            base = Math.round(seconds / times[displayStr]);
          } else {
            displayStr = 'ltSecond';
          }
        }

        return this.translate(displayStr, base);
      }

    }

    var bruteforceMatcher = (() => {
      return null;
    });

    var dateMatcher = (() => {
      return {
        warning: zxcvbnOptions.translations.warnings.dates,
        suggestions: [zxcvbnOptions.translations.suggestions.dates]
      };
    });

    const getDictionaryWarningPassword = (match, isSoleMatch) => {
      let warning = '';

      if (isSoleMatch && !match.l33t && !match.reversed) {
        if (match.rank <= 10) {
          warning = zxcvbnOptions.translations.warnings.topTen;
        } else if (match.rank <= 100) {
          warning = zxcvbnOptions.translations.warnings.topHundred;
        } else {
          warning = zxcvbnOptions.translations.warnings.common;
        }
      } else if (match.guessesLog10 <= 4) {
        warning = zxcvbnOptions.translations.warnings.similarToCommon;
      }

      return warning;
    };

    const getDictionaryWarningWikipedia = (match, isSoleMatch) => {
      let warning = '';

      if (isSoleMatch) {
        warning = zxcvbnOptions.translations.warnings.wordByItself;
      }

      return warning;
    };

    const getDictionaryWarningNames = (match, isSoleMatch) => {
      if (isSoleMatch) {
        return zxcvbnOptions.translations.warnings.namesByThemselves;
      }

      return zxcvbnOptions.translations.warnings.commonNames;
    };

    const getDictionaryWarning = (match, isSoleMatch) => {
      let warning = '';
      const dictName = match.dictionaryName;
      const isAName = dictName === 'lastnames' || dictName.toLowerCase().includes('firstnames');

      if (dictName === 'passwords') {
        warning = getDictionaryWarningPassword(match, isSoleMatch);
      } else if (dictName.includes('wikipedia')) {
        warning = getDictionaryWarningWikipedia(match, isSoleMatch);
      } else if (isAName) {
        warning = getDictionaryWarningNames(match, isSoleMatch);
      } else if (dictName === 'userInputs') {
        warning = zxcvbnOptions.translations.warnings.userInputs;
      }

      return warning;
    };

    var dictionaryMatcher = ((match, isSoleMatch) => {
      const warning = getDictionaryWarning(match, isSoleMatch);
      const suggestions = [];
      const word = match.token;

      if (word.match(START_UPPER)) {
        suggestions.push(zxcvbnOptions.translations.suggestions.capitalization);
      } else if (word.match(ALL_UPPER_INVERTED) && word.toLowerCase() !== word) {
        suggestions.push(zxcvbnOptions.translations.suggestions.allUppercase);
      }

      if (match.reversed && match.token.length >= 4) {
        suggestions.push(zxcvbnOptions.translations.suggestions.reverseWords);
      }

      if (match.l33t) {
        suggestions.push(zxcvbnOptions.translations.suggestions.l33t);
      }

      return {
        warning,
        suggestions
      };
    });

    var regexMatcher = (match => {
      if (match.regexName === 'recentYear') {
        return {
          warning: zxcvbnOptions.translations.warnings.recentYears,
          suggestions: [zxcvbnOptions.translations.suggestions.recentYears, zxcvbnOptions.translations.suggestions.associatedYears]
        };
      }

      return {
        warning: '',
        suggestions: []
      };
    });

    var repeatMatcher = (match => {
      let warning = zxcvbnOptions.translations.warnings.extendedRepeat;

      if (match.baseToken.length === 1) {
        warning = zxcvbnOptions.translations.warnings.simpleRepeat;
      }

      return {
        warning,
        suggestions: [zxcvbnOptions.translations.suggestions.repeated]
      };
    });

    var sequenceMatcher = (() => {
      return {
        warning: zxcvbnOptions.translations.warnings.sequences,
        suggestions: [zxcvbnOptions.translations.suggestions.sequences]
      };
    });

    var spatialMatcher = (match => {
      let warning = zxcvbnOptions.translations.warnings.keyPattern;

      if (match.turns === 1) {
        warning = zxcvbnOptions.translations.warnings.straightRow;
      }

      return {
        warning,
        suggestions: [zxcvbnOptions.translations.suggestions.longerKeyboardPattern]
      };
    });

    const defaultFeedback = {
      warning: '',
      suggestions: []
    };
    /*
     * -------------------------------------------------------------------------------
     *  Generate feedback ---------------------------------------------------------------
     * -------------------------------------------------------------------------------
     */

    class Feedback {
      constructor() {
        this.matchers = {
          bruteforce: bruteforceMatcher,
          date: dateMatcher,
          dictionary: dictionaryMatcher,
          regex: regexMatcher,
          repeat: repeatMatcher,
          sequence: sequenceMatcher,
          spatial: spatialMatcher
        };
        this.defaultFeedback = {
          warning: '',
          suggestions: []
        };
        this.setDefaultSuggestions();
      }

      setDefaultSuggestions() {
        this.defaultFeedback.suggestions.push(zxcvbnOptions.translations.suggestions.useWords, zxcvbnOptions.translations.suggestions.noNeed);
      }

      getFeedback(score, sequence) {
        if (sequence.length === 0) {
          return this.defaultFeedback;
        }

        if (score > 2) {
          return defaultFeedback;
        }

        const extraFeedback = zxcvbnOptions.translations.suggestions.anotherWord;
        const longestMatch = this.getLongestMatch(sequence);
        let feedback = this.getMatchFeedback(longestMatch, sequence.length === 1);

        if (feedback !== null && feedback !== undefined) {
          feedback.suggestions.unshift(extraFeedback);

          if (feedback.warning == null) {
            feedback.warning = '';
          }
        } else {
          feedback = {
            warning: '',
            suggestions: [extraFeedback]
          };
        }

        return feedback;
      }

      getLongestMatch(sequence) {
        let longestMatch = sequence[0];
        const slicedSequence = sequence.slice(1);
        slicedSequence.forEach(match => {
          if (match.token.length > longestMatch.token.length) {
            longestMatch = match;
          }
        });
        return longestMatch;
      }

      getMatchFeedback(match, isSoleMatch) {
        if (this.matchers[match.pattern]) {
          return this.matchers[match.pattern](match, isSoleMatch);
        }

        if (zxcvbnOptions.matchers[match.pattern] && 'feedback' in zxcvbnOptions.matchers[match.pattern]) {
          return zxcvbnOptions.matchers[match.pattern].feedback(match, isSoleMatch);
        }

        return defaultFeedback;
      }

    }

    /**
     * @link https://davidwalsh.name/javascript-debounce-function
     */
    var debounce = ((func, wait, isImmediate) => {
      let timeout;
      return function debounce(...args) {
        const context = this;

        const later = () => {
          timeout = undefined;

          if (!isImmediate) {
            func.apply(context, args);
          }
        };

        const shouldCallNow = isImmediate && !timeout;

        if (timeout !== undefined) {
          clearTimeout(timeout);
        }

        timeout = setTimeout(later, wait);

        if (shouldCallNow) {
          return func.apply(context, args);
        }

        return undefined;
      };
    });

    const time = () => new Date().getTime();

    const createReturnValue = (resolvedMatches, password, start) => {
      const feedback = new Feedback();
      const timeEstimates = new TimeEstimates();
      const matchSequence = scoring.mostGuessableMatchSequence(password, resolvedMatches);
      const calcTime = time() - start;
      const attackTimes = timeEstimates.estimateAttackTimes(matchSequence.guesses);
      return {
        calcTime,
        ...matchSequence,
        ...attackTimes,
        feedback: feedback.getFeedback(attackTimes.score, matchSequence.sequence)
      };
    };

    const main = (password, userInputs) => {
      if (userInputs) {
        zxcvbnOptions.extendUserInputsDictionary(userInputs);
      }

      const matching = new Matching();
      return matching.match(password);
    };

    const zxcvbn = (password, userInputs) => {
      const start = time();
      const matches = main(password, userInputs);

      if (matches instanceof Promise) {
        throw new Error('You are using a Promised matcher, please use `zxcvbnAsync` for it.');
      }

      return createReturnValue(matches, password, start);
    };
    const zxcvbnAsync = async (password, userInputs) => {
      const start = time();
      const matches = await main(password, userInputs);
      return createReturnValue(matches, password, start);
    };

    exports.debounce = debounce;
    exports.zxcvbn = zxcvbn;
    exports.zxcvbnAsync = zxcvbnAsync;
    exports.zxcvbnOptions = zxcvbnOptions;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({});
//# sourceMappingURL=zxcvbn-ts.js.map
