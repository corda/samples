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

`sendAttachment`: send and sync the attachment between parties

`downloadAttchment`: save the attachment file from node's serviceHub to local

![alt text](https://github.com/corda/samples/blob/release-V4/attachZIP/graph.png)

## Running the demo 
Deploy and run the nodes by:
```
./gradlew deployNodes
./build/nodes/runnodes
```
if you have any questions during setup, please go to https://docs.corda.net/getting-set-up.html for detailed setup instructions. 

Once all four nodes are started up, in Seller's node shell, run: 
```
flow start downloadAttachment receiver: Buyer
```
After this call, we already finished 
1. uploading a zip file to Seller's node
2. sending the zip file to Buyer's node

Now, lets move to Buyer's node shell, and run:
```
flow start downloadAttachment sender: Seller, path: file.zip
```
This command is telling the node to retrieve attachment from the transaction that is sent by `Seller`and download it as `file.zip` at the node root direction （⚠️ attachZIP/build/nodes/Buyer)


