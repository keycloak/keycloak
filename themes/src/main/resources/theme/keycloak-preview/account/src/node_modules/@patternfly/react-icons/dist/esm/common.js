import PropTypes from 'prop-types';
export const IconSize = {
  sm: 'sm',
  md: 'md',
  lg: 'lg',
  xl: 'xl'
};
export const propTypes = {
  color: PropTypes.string,
  size: PropTypes.oneOf(Object.keys(IconSize)),
  title: PropTypes.string,
  noVerticalAlign: PropTypes.bool
};
export const defaultProps = {
  color: 'currentColor',
  size: IconSize.sm,
  title: null,
  noVerticalAlign: false
};
export const getSize = size => {
  switch (size) {
    case IconSize.sm:
      return '1em';

    case IconSize.md:
      return '1.5em';

    case IconSize.lg:
      return '2em';

    case IconSize.xl:
      return '3em';

    default:
      return '1em';
  }
};
//# sourceMappingURL=common.js.map