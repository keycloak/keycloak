import createEmotion from '../';

const emotion0 = createEmotion({
  x: 5,
});
const emotion1 = createEmotion({
  y: 4,
  __SECRET_EMOTION__: emotion0,
});

const {
  flush,
  hydrate,
  cx,
  merge,
  getRegisteredStyles,
  css,
  injectGlobal,
  keyframes,
  sheet,
  caches,
} = createEmotion({});

flush();

hydrate([]);
hydrate(['123']);

cx();
cx(undefined);
cx(null);
cx(true);
cx('123');
cx('123', null, 'pf');
cx({
  abc: false,
  fp: true,
});
cx([]);
cx(['cl', {
  fp: true,
}]);

merge('abc def fpfp');

getRegisteredStyles([], 'abc');
getRegisteredStyles(['abc'], 'bcd');
getRegisteredStyles([], 'abc def fpfw');

css`
  height: 20px;
`;
css`
  color: ${'green'};
  font-size: ${10 + 4}px;
`;

css();
css(1);
css('abc');
css(true);

css([]);
css([1]);
css([['abc', 'asdf'], 'efw']);

css({
  ':active': {
    borderRadius: '2px',
    overflowAnchor: 'none',
    clear: ['both', 'left'],
  },
  '::before': {
    borderRadius: '2px',
  },
});

css(true, true);
css('fa', 1123);
css(['123'], 'asdf');

injectGlobal();
injectGlobal(30);
injectGlobal('this-is-class');
injectGlobal({});
injectGlobal([{
  animationDelay: '200ms',
}]);

keyframes();
keyframes({
  from: {
    marginLeft: '100%',
  },
  to: {
    marginLeft: '50%',
  },
});
keyframes([{
  from: {
    marginLeft: '100%',
  },
  to: {
    marginLeft: '50%',
  },
}, {
  '0%': {
    width: '100px',
  },
  '50%': {
    width: '50px',
  },
  '100%': {
    width: '120px',
  },
}]);

sheet.flush();
sheet.inject();
sheet.insert('');
sheet.speedy(false);

caches.inserted;
caches.key;
caches.nonce;
caches.registered;
