# OpenapiJsClient.NetworkApi

All URIs are relative to *http://localhost:9004/api/rest*

Method | HTTP request | Description
------------- | ------------- | -------------
[**networkNodes**](NetworkApi.md#networkNodes) | **GET** /network/nodes | 
[**networkNodesSelf**](NetworkApi.md#networkNodesSelf) | **GET** /network/nodes/self | 
[**networkNotaries**](NetworkApi.md#networkNotaries) | **GET** /network/notaries | 



## networkNodes

> [IoBluebankBraidCordaServicesSimpleNodeInfo] networkNodes(opts)



Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.

### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.NetworkApi();
let opts = {
  'hostAndPort': "hostAndPort_example", // String | 
  'x500Name': "x500Name_example" // String | 
};
apiInstance.networkNodes(opts, (error, data, response) => {
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
 **hostAndPort** | **String**|  | [optional] 
 **x500Name** | **String**|  | [optional] 

### Return type

[**[IoBluebankBraidCordaServicesSimpleNodeInfo]**](IoBluebankBraidCordaServicesSimpleNodeInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## networkNodesSelf

> IoBluebankBraidCordaServicesSimpleNodeInfo networkNodesSelf()



Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.

### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.NetworkApi();
apiInstance.networkNodesSelf((error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**IoBluebankBraidCordaServicesSimpleNodeInfo**](IoBluebankBraidCordaServicesSimpleNodeInfo.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## networkNotaries

> [NetCordaCoreIdentityParty] networkNotaries(opts)



### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.NetworkApi();
let opts = {
  'x500Name': "x500Name_example" // String | 
};
apiInstance.networkNotaries(opts, (error, data, response) => {
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
 **x500Name** | **String**|  | [optional] 

### Return type

[**[NetCordaCoreIdentityParty]**](NetCordaCoreIdentityParty.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

