# OpenapiJsClient.NetCordaCoreNodeServicesVaultStateMetadata

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**ref** | [**NetCordaCoreContractsStateRef**](NetCordaCoreContractsStateRef.md) |  | [optional] 
**contractStateClassName** | **String** |  | 
**recordedTime** | **String** | JSR310 encoded time representation of Instant | 
**consumedTime** | **String** | JSR310 encoded time representation of Instant | [optional] 
**status** | **String** |  | 
**notary** | [**NetCordaCoreIdentityAbstractParty**](NetCordaCoreIdentityAbstractParty.md) |  | [optional] 
**lockId** | **String** |  | [optional] 
**lockUpdateTime** | **String** | JSR310 encoded time representation of Instant | [optional] 
**relevancyStatus** | **String** |  | [optional] 
**constraintInfo** | [**NetCordaCoreNodeServicesVaultConstraintInfo**](NetCordaCoreNodeServicesVaultConstraintInfo.md) |  | [optional] 



## Enum: StatusEnum


* `UNCONSUMED` (value: `"UNCONSUMED"`)

* `CONSUMED` (value: `"CONSUMED"`)

* `ALL` (value: `"ALL"`)





## Enum: RelevancyStatusEnum


* `RELEVANT` (value: `"RELEVANT"`)

* `NOT_RELEVANT` (value: `"NOT_RELEVANT"`)

* `ALL` (value: `"ALL"`)




