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

import assert from 'assert';
import {buildServiceProxy} from './Utilities';

describe('ServiceProxyBasicConnectivityTest', () => {

  it('connect to a server, login, and call a simple method', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.add(5, 6))
        .then(result => assert.equal(11, result))
        .then(done, done)
    })
  }).timeout(0);

  it('that a method that throws is reported in the client', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.badjuju())
        .then(() => {
          throw new Error("method should have raised an error");
        })
        .catch(err => {
          assert.equal(err.codeDescription, "Server error");
          assert.equal(err.message, "I threw an exception");
        })
        .then(done, done)
    });
  }).timeout(0);

  it('that an unknown method raises an appropriate exception', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({username: 'admin', password: 'admin'})
        .then(() => proxy.unknownMethod())
        .then(() => {
          throw new Error("method should have raised an error");
        })
        .catch(err => {
          assert.equal(err.codeDescription, "Method not found");
          assert.ok(err.message.includes('unknownMethod'));
        })
        .then(done, done)
    });
  }).timeout(0);
});




