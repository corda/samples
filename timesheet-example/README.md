<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Timesheet CorDapp

This sample CorDapp demonstrates invoice processing using Corda.
The CorDapp allows Contractor to issue invoice to a company for his services. The contractor 
inputs the number of hours worked and the payment against the invoice is made by the Company.
The payrate is fetched from an Oracle while payment of invoices. 

# Pre-requisites:
  
See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

# Interacting with the nodes:

To issue an invoice from the contractor to the company, go to the Contractor node and input:
    
    flow start IssueInvoiceFlow hoursWorked: 5, date: 2020-01-08 , otherParty: MegaCorp 1
    
In order for the invoice to be paid, you first need to retrieve the UUID of the invoice. From the MegaCorp 1 node run:

    run vaultQuery contractStateType: com.example.state.InvoiceState

Find the linearID of the invoice state (it should be 32 characters). Using that, run from the MegaCorp 1 node:

    flow start PayInvoiceFlow invoiceId: <UUID of invoice state>

Run the vaultQuery command on the Contractor node to validate that the cash payment against the invoice has been 
transferred to the Contractor:

    run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State    