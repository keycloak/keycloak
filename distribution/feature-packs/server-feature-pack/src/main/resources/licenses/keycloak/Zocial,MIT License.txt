# Zocial CSS social buttons

I basically rewrote this entire set so they are full vector buttons, meaning:

- @font-face icons
- custom font file for all social icons
- icon font use private unicode spaces for accessibility
- em sizing based on button font-size
- support for about 83 different services
- buttons and icons supported
- no raster images (sweet)
- works splendidly on any browser supporting @font-face
- CSS3 degrades gracefully in IE8 and below etc.
- also includes generic icon-less primary and secondary buttons

## How to use these buttons

	<button class="zocial facebook">Button label here</button>

or

	<a class="zocial twitter">Button label</a>

- Can be any element e.g. `a`, `div`, `button` etc.
- Add class of `.zocial`
- Add class for name of service e.g. `.dropbox`, `.twitter`, `.github`
- Done :-)

Check out [zocial.smcllns.com](http://zocial.smcllns.com) for demo and code examples.

There's also a LESS version from @gustavohenke [here](https://github.com/gustavohenke/zocial-less)

Problems, questions or requests to [@smcllns](http://twitter.com/smcllns)

## How to contribute

Install [Font Custom](http://fontcustom.com/#installation)

Run `fontcustom compile` to build the font.

New fonts are defined by adding them in the `svg/` folder. For color settings
see the `templates/zocial.css` file.

## License

Under [MIT License](http://opensource.org/licenses/mit-license.php)
