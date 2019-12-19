/*
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"use strict";

function buildURLClass() {
  if (typeof(document) !== 'undefined' && typeof(window) !== 'undefined') {
    return BrowserURL;
  } else {
    const { URL } =require('url');
    return URL;
  }
}

class BrowserURL {
  constructor(url) {
    const parser = document.createElement('a');
    parser.href = url;
    this.protocol = parser.protocol;
    this.host = parser.hostname + ":" + parser.port;
    this.hostname = parser.hostname;
    this.port = parser.port
  }
}

const url = buildURLClass();
export default url;