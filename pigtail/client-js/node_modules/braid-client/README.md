# braid-client NPM module

## Short Example

```bash
npm install --save braid-client
```

followed by something along the lines of:

```javascript
const Proxy = require('braid-client').Proxy;

const alpha1 = new Proxy({url: 'https://alpha-one.cordite.foundation:8080/api/'}, onOpen, onClose, onError, {strictSSL: false})

let saltedDaoName = 'testDao-'+new Date().getTime()

function onOpen() {
    console.log("ooh - worked")
    console.log(new Date().getTime())
    alpha1.dao.daoInfo(saltedDaoName).then(daos => {
        console.log("there were", daos.length, "existing doas with name", saltedDaoName )
        
        return alpha1.dao.createDao.docs()

    }).catch(error => {
        console.error(error)
    })
}

function onClose() {
    console.log("closed")
}

function onError(err) {
    console.error(err)
}
```

Note that this hits the live cordite alpha.

## Features

Allows for the calling of any service method exposed by the end point.
If the service method is not available, the module emits a message and URL to the developer console.
Clicking on the URL takes the UI developer to the Braid Editor to add the new method the service.

## Build

```npm run build```

