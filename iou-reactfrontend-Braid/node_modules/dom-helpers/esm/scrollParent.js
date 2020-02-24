/* eslint-disable no-cond-assign, no-continue */
import css from './css';
import height from './height';
import isDocument from './isDocument';
export default function scrollPrarent(node) {
  var position = css(node, 'position');
  var excludeStatic = position === 'absolute';
  var ownerDoc = node.ownerDocument;
  if (position === 'fixed') return ownerDoc || document; // @ts-ignore

  while ((node = node.parentNode) && !isDocument(node)) {
    var isStatic = excludeStatic && css(node, 'position') === 'static',
        style = (css(node, 'overflow') || '') + (css(node, 'overflow-y') || '') + css(node, 'overflow-x');
    if (isStatic) continue;
    if (/(auto|scroll)/.test(style) && height(node) < node.scrollHeight) return node;
  }

  return document;
}