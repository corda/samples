# OpenapiJsClient.NetCordaCoreTransactionsSignedTransaction

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**txBits** | [**NetCordaCoreSerializationSerializedBytesNetCordaCoreTransactionsCoreTransaction**](NetCordaCoreSerializationSerializedBytesNetCordaCoreTransactionsCoreTransaction.md) |  | [optional] 
**sigs** | [**[NetCordaCoreCryptoTransactionSignature]**](NetCordaCoreCryptoTransactionSignature.md) |  | 
**id** | **String** | Base 58 Encoded Secure Hash | 
**tx** | [**NetCordaCoreTransactionsWireTransaction**](NetCordaCoreTransactionsWireTransaction.md) |  | [optional] 
**coreTransaction** | [**NetCordaCoreTransactionsCoreTransaction**](NetCordaCoreTransactionsCoreTransaction.md) |  | [optional] 
**inputs** | [**[NetCordaCoreContractsStateRef]**](NetCordaCoreContractsStateRef.md) |  | 
**references** | [**[NetCordaCoreContractsStateRef]**](NetCordaCoreContractsStateRef.md) |  | 
**notary** | [**NetCordaCoreIdentityParty**](NetCordaCoreIdentityParty.md) |  | [optional] 
**networkParametersHash** | **String** | Base 58 Encoded Secure Hash | [optional] 
**notaryChangeTransaction** | **Boolean** |  | 
**missingSigners** | **[String]** |  | 


