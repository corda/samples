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

'use strict';
const xhr = require('request');

import DynamicProxy from './DynamicServiceProxy';

export { default as ServiceProxy } from './ServiceProxy';

export class Proxy {
  constructor(config, onOpen, onClose, onError, options) {
    if (!config.url) {
      throw "config must include url property e.g. https://localhost:8080"
    }

    if (typeof options === 'undefined') {
      options = {};
    }

    if (typeof options.strictSSL === 'undefined') {
      options.strictSSL = true;
    }
    if (!options.strictSSL) {
      if (typeof process !== 'undefined' && typeof process.env !== 'undefined') {
        // NOTE: rather nasty - to be used only in local dev for self-signed certificates
        process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
      }
    }

    const that = this;
    let errors = 0;
    let connections = 0;
    let requiredConnections = 0;

    // --- PRIVATE FUNCTIONS ---

    function onInternalOpen() {
      if (++connections === requiredConnections && errors === 0 && onOpen) {
        onOpen()
      }
    }

    function failed(e) {
      if (onError) {
        onError(e)
      } else {
        console.error(e)
      }
    }

    function onInternalClose() {
      if (connections <= 0 && errors === 0 && onClose) {
        onClose()
      }
    }

    function onInternalError(e) {
      console.error(typeof(e), e);
      if (++errors === 1 && onError) {
        onError(e)
      }
    }

    function bootstrap() {
      const url = config.url;
      xhr({
        method: "get",
        uri: url,
        strictSSL: options.strictSSL,
        rejectUnauthorized: !options.strictSSL,
        headers: {
          "Content-Type": "application/json"
        }
      }, function(err, resp, body) {
        if (err) {
          clearCredentials();
          failed("failed to get services descriptor: " + err)
        } else if (resp) {
          bindServices(body);
        }
      })
    }

    function bindServices(body) {
      const services = JSON.parse(body)
      const serviceNames = Object.keys(services);
      requiredConnections = serviceNames.length;
      for (let idx = 0; idx < serviceNames.length; ++idx) {
        const serviceName = serviceNames[idx];
        that[serviceName] = new DynamicProxy(config, serviceName, onInternalOpen, onInternalClose, onInternalError, options);
      }
      clearCredentials();
    }

    function clearCredentials() {
      config.credentials = null;
    }

    // --- PUBLIC FUNCTIONS ---

    // --- INITIALISATION ---
    bootstrap();
  }
}
