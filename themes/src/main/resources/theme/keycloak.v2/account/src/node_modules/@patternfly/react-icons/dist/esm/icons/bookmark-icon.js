import { createIcon } from '../createIcon';

export const BookmarkIconConfig = {
  name: 'BookmarkIcon',
  height: 512,
  width: 384,
  svgPath: 'M0 512V48C0 21.49 21.49 0 48 0h288c26.51 0 48 21.49 48 48v464L192 400 0 512z',
  yOffset: 0,
  xOffset: 0,
};

export const BookmarkIcon = createIcon(BookmarkIconConfig);

export default BookmarkIcon;