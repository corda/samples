# OpenapiJsClient.CordappsApi

All URIs are relative to *http://localhost:9004/api/rest*

Method | HTTP request | Description
------------- | ------------- | -------------
[**cordapps**](CordappsApi.md#cordapps) | **GET** /cordapps | 
[**cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow**](CordappsApi.md#cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow) | **POST** /cordapps/bootcamp-openapi/flows/bootcamp.GetAllTokensFlow | 
[**cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator**](CordappsApi.md#cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator) | **POST** /cordapps/bootcamp-openapi/flows/bootcamp.TokenIssueFlowInitiator | 
[**cordappsCordappFlows**](CordappsApi.md#cordappsCordappFlows) | **GET** /cordapps/{cordapp}/flows | 
[**cordappsProgressTracker**](CordappsApi.md#cordappsProgressTracker) | **GET** /cordapps/progress-tracker | 



## cordapps

> [String] cordapps()



### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.CordappsApi();
apiInstance.cordapps((error, data, response) => {
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

**[String]**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow

> Object cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow(body, opts)



### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.CordappsApi();
let body = null; // Object | payload
let opts = {
  'invocationId': "invocationId_example" // String | 
};
apiInstance.cordappsBootcampOpenapiFlowsBootcampGetAllTokensFlow(body, opts, (error, data, response) => {
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
 **body** | **Object**| payload | 
 **invocationId** | **String**|  | [optional] 

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator

> NetCordaCoreTransactionsSignedTransaction cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator(generatedBootcampTokenIssueFlowInitiatorPayload, opts)



### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.CordappsApi();
let generatedBootcampTokenIssueFlowInitiatorPayload = new OpenapiJsClient.GeneratedBootcampTokenIssueFlowInitiatorPayload(); // GeneratedBootcampTokenIssueFlowInitiatorPayload | payload
let opts = {
  'invocationId': "invocationId_example" // String | 
};
apiInstance.cordappsBootcampOpenapiFlowsBootcampTokenIssueFlowInitiator(generatedBootcampTokenIssueFlowInitiatorPayload, opts, (error, data, response) => {
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
 **generatedBootcampTokenIssueFlowInitiatorPayload** | [**GeneratedBootcampTokenIssueFlowInitiatorPayload**](GeneratedBootcampTokenIssueFlowInitiatorPayload.md)| payload | 
 **invocationId** | **String**|  | [optional] 

### Return type

[**NetCordaCoreTransactionsSignedTransaction**](NetCordaCoreTransactionsSignedTransaction.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## cordappsCordappFlows

> [String] cordappsCordappFlows(cordapp)



### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.CordappsApi();
let cordapp = "cordapp_example"; // String | 
apiInstance.cordappsCordappFlows(cordapp, (error, data, response) => {
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
 **cordapp** | **String**|  | 

### Return type

**[String]**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## cordappsProgressTracker

> IoBluebankBraidCordaServerProgressProgressNotification cordappsProgressTracker()



Connect to the Progress Tracker. This call will return chunked responses of all progress trackers

### Example

```javascript
import OpenapiJsClient from 'openapi-js-client';

let apiInstance = new OpenapiJsClient.CordappsApi();
apiInstance.cordappsProgressTracker((error, data, response) => {
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

[**IoBluebankBraidCordaServerProgressProgressNotification**](IoBluebankBraidCordaServerProgressProgressNotification.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

