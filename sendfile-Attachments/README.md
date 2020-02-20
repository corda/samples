# attachZIP -- Attachment Demo

<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

## Introduction 
This Cordapp shows how to upload and download an attachment via flow. 
In this Cordapp, there are two parties: 
* Seller: sends an invoice (with attachment) to Buyer
* Buyer: receive the the invoice and be able to download the attached zip file to their local machine 

There is one state `InvoiceState` and two flows `sendAttachment` and `downloadAttachment`. The flow logic is the following:

* `sendAttachment`: send and sync the attachment between parties
  1. Uploading attachment from local 
  2. Attaching the accachmentID to the transaction 
  3. Storing the attached file into attachment service at the counterparty's node (Automatically check if it already exists or not. If it does, do nothing; if not, download the attached file from the conterparty.)

* `downloadAttchment`: save the attachment file from node's serviceHub to local
  1. signing the attachment service in the node to download the file via attachmentID

![alt text](https://github.com/corda/samples/blob/add-samples/sendfile-Attachments/graph.png)

## Running the demo 
Deploy and run the nodes by:

Java
```
./gradlew workflows-java:deployNodes
.workflows-java/build/nodes/runnodes
```
or Kotlin
```$xslt
./gradlew workflows-kotlin:deployNodes
.workflows-kotlin/build/nodes/runnodes
```

if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions. 

Once all three nodes are started up, in Seller's node shell, run: 
```
flow start SendAttachment receiver: Buyer
```
After this call, we already finished 
1. uploading a zip file to Seller's node
2. sending the zip file to Buyer's node

Now, lets move to Buyer's node shell, and run:
```
flow start DownloadAttachment sender: Seller, path: file.zip
```
This command is telling the node to retrieve attachment from the transaction that is sent by `Seller`and download it as `file.zip` at the node root direction （⚠️ attachZIP/build/nodes/Buyer)

## Notes: 
* This uploaded file is hardcoded into the flow. 
* The transaction retrieving is also hardcoded to retrieve the first state that being stored in the vault. 

