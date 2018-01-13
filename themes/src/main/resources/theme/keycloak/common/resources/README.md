Management of javascript libraries
===================================================

Javascript libraries under the *./lib* directory are not managed.  These 
libraries are not available in the public npm repo and are thus checked into 
GitHub.

Javascript libraries under *./node_modules* directory are managed with yarn.  

THESE LIBRARIES SHOULD BE CHECKED INTO GITHUB UNTIL KEYCLOAK-5324 and KEYCLOAK-5392
ARE RESOLVED.

Adding or Removing javascript libraries
---------------------------------------
To add/remove/update javascript libraries you should always use yarn so that 
the yarn.lock file will be updated.  Then, just check in the modified version 
of package.json and yarn.lock.  To do this, you should locally install 
nodejs/npm and yarn.

Do not use *npm install --save*. If you try to update a dependency using 
package.json and fail to update yarn.lock, then the next build will fail.

To locally install nodejs/npm and yarn, see:

* [Install nodejs and npm](https://www.npmjs.com/get-npm)
* [Install yarn](https://yarnpkg.com/lang/en/docs/install/)
