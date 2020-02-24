# OpenapiJsClient.NetCordaCoreNodeServicesVaultQueryCriteriaVaultQueryCriteria

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**status** | **String** |  | 
**contractStateTypes** | **[String]** |  | [optional] 
**stateRefs** | [**[NetCordaCoreContractsStateRef]**](NetCordaCoreContractsStateRef.md) |  | [optional] 
**notary** | [**[NetCordaCoreIdentityAbstractParty]**](NetCordaCoreIdentityAbstractParty.md) |  | [optional] 
**softLockingCondition** | [**NetCordaCoreNodeServicesVaultQueryCriteriaSoftLockingCondition**](NetCordaCoreNodeServicesVaultQueryCriteriaSoftLockingCondition.md) |  | [optional] 
**timeCondition** | [**NetCordaCoreNodeServicesVaultQueryCriteriaTimeCondition**](NetCordaCoreNodeServicesVaultQueryCriteriaTimeCondition.md) |  | [optional] 
**relevancyStatus** | **String** |  | 
**constraintTypes** | **[String]** |  | 
**constraints** | [**[NetCordaCoreNodeServicesVaultConstraintInfo]**](NetCordaCoreNodeServicesVaultConstraintInfo.md) |  | 
**participants** | [**[NetCordaCoreIdentityAbstractParty]**](NetCordaCoreIdentityAbstractParty.md) |  | [optional] 
**externalIds** | **[String]** |  | 
**exactParticipants** | [**[NetCordaCoreIdentityAbstractParty]**](NetCordaCoreIdentityAbstractParty.md) |  | [optional] 



## Enum: StatusEnum


* `UNCONSUMED` (value: `"UNCONSUMED"`)

* `CONSUMED` (value: `"CONSUMED"`)

* `ALL` (value: `"ALL"`)





## Enum: RelevancyStatusEnum


* `RELEVANT` (value: `"RELEVANT"`)

* `NOT_RELEVANT` (value: `"NOT_RELEVANT"`)

* `ALL` (value: `"ALL"`)





## Enum: [ConstraintTypesEnum]


* `ALWAYS_ACCEPT` (value: `"ALWAYS_ACCEPT"`)

* `HASH` (value: `"HASH"`)

* `CZ_WHITELISTED` (value: `"CZ_WHITELISTED"`)

* `SIGNATURE` (value: `"SIGNATURE"`)




