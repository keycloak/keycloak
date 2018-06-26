var fs = require('fs');

/*!
 * add 1 to suffix number
 * @param {String} name file basename
 * @return {String} name with addition
 */
var addOne = function (name) {
	name = name.split( '_' );
	var n = Number( name.pop()) + 1;
	name.push( n );
	return name.join( '_' );
};


/*!
 * detect if name has a number suffix after '_'
 * (example: picture_5.jpg)
 * @param  {string}  name basename to examinate
 * @return {Boolean|Number}      if has not suffix: false, else: name with addition
 */
var hasSuffix = function (name) {
	var suffix, splitted;
	if (!isNaN( name )) {
		return false;
	} else {
		splitted = name.split( '_' );
		if (splitted.length > 1) {
			suffix = splitted.pop();
			if (isNaN( suffix )) {
				return false;
			} else {
				return addOne( name );
			}
		} else {
			return false;
		}
	}
};

/*!
 * separate basename from file path and send it to rename
 * @param  {String} route route of the file
 * @return {String}       new name
 */
var newName = function ( route ) {
	// get filename
	route = route.split( '/' );
	var filename = route.pop();
	var splitted = filename.split( '.' );
	var basename = splitted.shift();
	var ext = splitted.join( '.' );
	var suffix = hasSuffix( basename );
	// check if filefileName has suffix
	if (suffix) {
		basename = suffix;
	} else {
		basename = basename + '_1';
	}
	filename = [basename, ext].join( '.' );
	route.push( filename );
	return route.join('/');
};

/*!
 * detects if file route exist and send it to rename
 * @param  {String} route file path
 * @return {String}       unique path
 */
var finalName = function (route) {
	if (fs.existsSync( route )) {
		return finalName( newName( route ));
	} else {
		return route;
	}
};




module.exports = finalName;
