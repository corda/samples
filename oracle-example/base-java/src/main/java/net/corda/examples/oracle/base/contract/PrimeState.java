package net.corda.examples.oracle.base.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// If 'n' is a natural number N then 'nthPrime' is the Nth prime.
// `Requester` is the Party that will store this fact in its vault.
@BelongsToContract(PrimeContract.class)
public class PrimeState implements ContractState {
    private final Integer n;
    private final Integer nthPrime;
    private final AbstractParty requester;

    public PrimeState(Integer n, Integer nthPrime, AbstractParty requester) {
        this.n = n;
        this.nthPrime = nthPrime;
        this.requester = requester;
    }

    public Integer getN() {
        return n;
    }

    public Integer getNthPrime() {
        return nthPrime;
    }

    public AbstractParty getRequester() {
        return requester;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(requester);
    }

    @Override
    public String toString() {
        return "the " + n + "th prime number is " + nthPrime;
    }
}
