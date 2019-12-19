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

import assert from 'assert'
import {buildServiceProxy} from './Utilities';

/**
 * Tests for authentication
 */
describe('ServiceProxyAuthTest', () => {

  it('if we do not login, we fail', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.add(5, 6)
        .then(() => {
          throw new Error("should have raised a not authenticated error");
        })
        .catch(err => {
          assert.equal(err.codeDescription, "Server error");
          assert.ok(err.message.includes('not authenticated'));
        })
        .then(done, done)
    })
  }).timeout(0);

  it('if we provide the wrong credentials, login fails', (done) => {
    buildServiceProxy(done, proxy => {
      proxy.login({ username: 'admin', password: 'wrongpassword'})
        .then(() => {
          throw new Error("should have raised a not authenticated error");
        })
        .catch(err => {
          // console.log(err);
          assert.equal(err.codeDescription, "Server error");
          assert.ok(err.message.includes('failed to authenticate'));
        })
        .then(done, done)
    })
  }).timeout(0);
});
