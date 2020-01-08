package com.example.flow;


import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import java.security.PublicKey;


@InitiatingFlow
class SignRate extends FlowLogic<TransactionSignature> {
    private TransactionBuilder tx;
    private Party oracle;
    private FilteredTransaction partialMerkleTree;

    public SignRate(TransactionBuilder tx, Party oracle, FilteredTransaction partialMerkleTree) {
        this.tx = tx;
        this.oracle = oracle;
        this.partialMerkleTree = partialMerkleTree;
    }

    @Suspendable
    @Override
    public TransactionSignature call() throws FlowException {
        FlowSession oracleSession = initiateFlow(oracle);
        UntrustworthyData resp = oracleSession.sendAndReceive(TransactionSignature.class,partialMerkleTree);
        return (TransactionSignature) resp.unwrap( sig ->{
            PublicKey k =  oracleSession.getCounterparty().getOwningKey();
            TransactionSignature f = (TransactionSignature) sig;
            if(ImmutableList.of(f.getBy()).contains(k)){
                tx.toWireTransaction(getServiceHub()).checkSignature((TransactionSignature) sig);
                return sig;
            }
            return null;
        });
    }
}