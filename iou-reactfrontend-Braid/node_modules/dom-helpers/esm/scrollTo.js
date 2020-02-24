/* eslint-disable no-nested-ternary */
import { cancel, request } from './animationFrame';
import height from './height';
import getWindow from './isWindow';
import getOffset from './offset';
import getScrollParent from './scrollParent';
import scrollTop from './scrollTop';
export default function scrollTo(selected, scrollParent) {
  var offset = getOffset(selected);
  var poff = {
    top: 0,
    left: 0
  };
  var selectedTop;
  var selectedHeight, bottom;
  if (!selected) return undefined;
  var list = scrollParent || getScrollParent(selected);
  var isWin = getWindow(list);
  var listScrollTop = scrollTop(list);
  var listHeight = height(list, true);
  if (!isWin) poff = getOffset(list);
  offset = {
    top: offset.top - poff.top,
    left: offset.left - poff.left,
    height: offset.height,
    width: offset.width
  };
  selectedHeight = offset.height;
  selectedTop = offset.top + (isWin ? 0 : listScrollTop);
  bottom = selectedTop + selectedHeight;
  listScrollTop = listScrollTop > selectedTop ? selectedTop : bottom > listScrollTop + listHeight ? bottom - listHeight : listScrollTop;
  var id = request(function () {
    return scrollTop(list, listScrollTop);
  });
  return function () {
    return cancel(id);
  };
}