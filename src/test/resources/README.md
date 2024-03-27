### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `postman-environment.json` has **values** array that has fields named **basePath** whose **value** is currently set to `/dx/apd/dmp/v1`, **dxAuthBasePath** with value `/auth/v1`.
- These value(s) could be changed according to the deployment and then the collection with the `postman-environment.json` file can be uploaded to Postman
- For the changing the **basePath**, **dxAuthBasePath** value in postman after importing the collection and environment files, locate `RS Environment` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variable could be changed

### Adding configurable variables
- The following user client credentials could be added in the environment file
by populated to generate respective token : [`providerClientID`, `providerClientSecret`, `consumerClientId`,
`consumerClientSecret`, `providerDelegateClientId`, `providerDelegateClientSecret`, 
`consumerDelegateClientId`, `consumerDelegateClientSecret`, `providerDelegateId`,
`consumerDelegateId`]
- The `resourceId` could be changed to required resourceId based on which products could be fetched 
- `POST /verify` endpoint uses a bearer authToken which could be added in `authToken`
- The base url is set to `https://dmp-apd.iudx.io` in a variable named `baseUrl` for the DX DMP APD server and the value could be changed accordingly
- The base url is set to `https://authvertx.iudx.io` in a variable named `authEndpoint` for the DX AAA server [link to the server](https://github.com/datakaveri/iudx-aaa-server) and the value could be changed accordingly