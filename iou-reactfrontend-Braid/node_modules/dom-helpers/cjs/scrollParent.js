"use strict";

var _interopRequireDefault = require("@babel/runtime/helpers/interopRequireDefault");

exports.__esModule = true;
exports.default = scrollPrarent;

var _css = _interopRequireDefault(require("./css"));

var _height = _interopRequireDefault(require("./height"));

var _isDocument = _interopRequireDefault(require("./isDocument"));

/* eslint-disable no-cond-assign, no-continue */
function scrollPrarent(node) {
  var position = (0, _css.default)(node, 'position');
  var excludeStatic = position === 'absolute';
  var ownerDoc = node.ownerDocument;
  if (position === 'fixed') return ownerDoc || document; // @ts-ignore

  while ((node = node.parentNode) && !(0, _isDocument.default)(node)) {
    var isStatic = excludeStatic && (0, _css.default)(node, 'position') === 'static',
        style = ((0, _css.default)(node, 'overflow') || '') + ((0, _css.default)(node, 'overflow-y') || '') + (0, _css.default)(node, 'overflow-x');
    if (isStatic) continue;
    if (/(auto|scroll)/.test(style) && (0, _height.default)(node) < node.scrollHeight) return node;
  }

  return document;
}

module.exports = exports["default"];