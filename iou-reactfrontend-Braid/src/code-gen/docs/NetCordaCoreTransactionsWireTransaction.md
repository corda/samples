# OpenapiJsClient.NetCordaCoreTransactionsWireTransaction

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**privacySalt** | [**NetCordaCoreContractsPrivacySalt**](NetCordaCoreContractsPrivacySalt.md) |  | [optional] 
**attachments** | **[String]** |  | 
**inputs** | [**[NetCordaCoreContractsStateRef]**](NetCordaCoreContractsStateRef.md) |  | 
**references** | [**[NetCordaCoreContractsStateRef]**](NetCordaCoreContractsStateRef.md) |  | 
**outputs** | [**[NetCordaCoreContractsTransactionStateNetCordaCoreContractsContractState]**](NetCordaCoreContractsTransactionStateNetCordaCoreContractsContractState.md) |  | 
**commands** | [**[NetCordaCoreContractsCommandObject]**](NetCordaCoreContractsCommandObject.md) |  | 
**notary** | [**NetCordaCoreIdentityParty**](NetCordaCoreIdentityParty.md) |  | [optional] 
**timeWindow** | [**NetCordaCoreContractsTimeWindow**](NetCordaCoreContractsTimeWindow.md) |  | [optional] 
**networkParametersHash** | **String** | Base 58 Encoded Secure Hash | [optional] 
**id** | **String** | Base 58 Encoded Secure Hash | 
**requiredSigningKeys** | **[String]** |  | 
**groupHashescore** | **[String]** |  | 
**groupsMerkleRootscore** | **{String: String}** |  | 
**availableComponentNoncescore** | **{String: [String]}** |  | 
**availableComponentHashescore** | **{String: [String]}** |  | 


