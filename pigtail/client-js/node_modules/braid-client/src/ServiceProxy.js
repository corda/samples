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

import JsonRPC from './JsonRpcClient'

/**
 * Provides a dynamic proxy that intercepts all method calls and forwards them to a braid JSONRpc service
 * Handles client use-cases for Promise and Observable style calls
 */
class BraidServiceProxy {
  constructor(url, onOpen, onClose, onError, options) {
    const that = this;
    if (!options) {
      options = {}
    }
    options.noCredentials = true;
    let client = new JsonRPC(url, options);
    client.onOpen = onOpen;
    client.onClose = onClose;
    client.onError = onError;

    // --- PRIVATE FUNCTIONS ---

    function bindCallbacks(args) {
      if (!args) return null;
      const last3Fns = args.slice(-3).filter(item => {
        return typeof item === 'function'
      });
      if (last3Fns.length === 0) return null;
      return {
        onNext: last3Fns[0],
        onError: last3Fns[1],
        onCompleted: last3Fns[2]
      }
    }

    function invokeForPromise(method, args) {
      args = massageArgs(args);
      return client.invoke(method, args).then(result => result, onErrorTrap);
    }

    function invokeForStream(method, args, callbacks) {
      const noFunctionArgs = massageArgs(args.filter(item => {
        return typeof item !== 'function'
      }));
      return client.invokeForStream(method, noFunctionArgs,
        callbacks.onNext,
        onErrorWrapper(callbacks.onError),
        callbacks.onCompleted);
    }

    function massageArgs(args) {
      if (args) {
        if (args.length === 0) {
          args = null;
        }
      }
      return args;
    }

    function onErrorWrapper(onError) {
      return (err) => {
        try {
          onErrorTrap(err)
        } catch(err2) {
          if (onError) {
            onError(err2);
          } else {
            console.error(err2);
          }
        }
      }
    }

    function onErrorTrap(err) {
      if (typeof document !== 'undefined' && err.jsonRPCError && err.jsonRPCError.code === -32601) {
        console.log("%cBraid: %c" + err.message + "\n%cCreate a stub here: " + uri(),
          "font-family: sans-serif; font-size: 14px; font-weight: bold;",
          "",
          "font-weight: bold;");
        throw Error(err.message)
      } else {
        throw err;
      }
    }

    function uri() {
      // parse url
      const uri = document.createElement('a');
      uri.href = url;
      const base = "http://" + uri.hostname + ":" + uri.port;
      const serviceName = uri.pathname.split("/").filter(i => i.length > 0).pop();
      if (serviceName !== undefined && serviceName !== null) {
        return base + "/?service=" + serviceName
      } else {
        return base;
      }
    }

    // --- PUBLIC FUNCTIONS ---

    /**
     * Invoke a method on the bound service with arguments. If the arguments do not end with a set of functions, the
     * method returns a Promise. Optionally the client may pass a set of functions (at least one), to receive Observable
     * style notifications.
     *
     * A method on the server side is free to return any of the following:
     * - A single value (primitive or object)
     * - A Future/Promise/Single object representing the monad on a potentially asynchronous operation
     * - An Observable representing a monad on a potentially asynchronous operation return a potentially unbounded stream of results
     *
     * @param method - the method name on the service to call
     * @param args - the arguments to be passed. The list of arguments can provide one or more of the following ordered callback functions
     * @param args[onNext(item)] - callback per item of result returned from the method. The 'item' parameter will hold the respective value object.
     * @param args[onError(error)] - callback if the stream of results has terminated with an error condition. The 'error' parameter will hold the respective error object.
     * @param args[onCompleted()] - a callback when the stream has successfully terminated.
     *
     */
    that.invoke = function(method, args) {
      const callbacks = bindCallbacks(args);
      if (callbacks) {
        return invokeForStream(method, args, callbacks);
      } else {
        return invokeForPromise(method, args);
      }
    }

  }
}

/**
 * Factory function to create a javascript proxy that dispatches ot a BraidServiceProxy object
 * @param url - the url to bind to a service. e.g. https://localhost:8080/api/jsonrpc/myservice
 * @param onOpen - a callback when connection to the service has been successfully made
 * @param onClose - a callback when the connection to service has been closed
 * @param onError - a callback when the connection the service has failed
 * @param transportOptions - these are options to control the SockJS transport - see https://github.com/sockjs/sockjs-client
 *                           also, includes the flag strictSSL which defaults to true.
 * @returns {Proxy}
 */
export default function(url, onOpen, onClose, onError, transportOptions) {
  return new Proxy(new BraidServiceProxy(url, onOpen, onClose, onError, transportOptions), {
    get: (target, propKey) => {
      return function (...args) {
        return target.invoke(propKey, args)
      }
    }
  });
};

