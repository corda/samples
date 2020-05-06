# OpenapiJsClient.VaultApi

All URIs are relative to *http://localhost:9004/api/rest*

Method | HTTP request | Description
------------- | ------------- | -------------
[**vaultVaultQuery**](VaultApi.md#vaultVaultQuery) | **GET** /vault/vaultQuery | 
[**vaultVaultQueryBy**](VaultApi.md#vaultVaultQueryBy) | **POST** /vault/vaultQueryBy | 



## vaultVaultQuery

> NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState vaultVaultQuery(opts)



Queries the vault for contract states of the supplied type

### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.VaultApi();
let opts = {
  'contractStateType': "contractStateType_example" // String | 
};
apiInstance.vaultVaultQuery(opts, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contractStateType** | **String**|  | [optional] 

### Return type

[**NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState**](NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## vaultVaultQueryBy

> NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState vaultVaultQueryBy(ioVertxExtAuthUser, opts)



Queries the vault

### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.VaultApi();
let ioVertxExtAuthUser = new OpenapiJsClient.IoVertxExtAuthUser(); // IoVertxExtAuthUser | user
let opts = {
  'vault': new OpenapiJsClient.IoBluebankBraidCordaServicesVaultVaultQuery() // IoBluebankBraidCordaServicesVaultVaultQuery | 
};
apiInstance.vaultVaultQueryBy(ioVertxExtAuthUser, opts, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ioVertxExtAuthUser** | [**IoVertxExtAuthUser**](IoVertxExtAuthUser.md)| user | 
 **vault** | [**IoBluebankBraidCordaServicesVaultVaultQuery**](.md)|  | [optional] 

### Return type

[**NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState**](NetCordaCoreNodeServicesVaultPageNetCordaCoreContractsContractState.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

