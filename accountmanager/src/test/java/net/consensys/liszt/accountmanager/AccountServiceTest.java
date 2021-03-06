package net.consensys.liszt.accountmanager;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.*;
import net.consensys.liszt.core.common.RTransfer;
import net.consensys.liszt.core.crypto.Hash;
import net.consensys.liszt.core.crypto.PublicKey;
import net.consensys.liszt.core.crypto.Signature;
import org.junit.Before;
import org.junit.Test;

public class AccountServiceTest {

  private static AccountService accountService;
  private static PublicKey alice = new PublicKey("Alice");
  private static PublicKey bob = new PublicKey("Bob");
  private static PublicKey zac = new PublicKey("Zac");
  private static PublicKey kate = new PublicKey("Kate");

  private static Hash initialRootHash;

  @Before
  public void setUp() {
    AccountStateProvider accountsStateProvider = new InMemoryAccountsStateProvider();
    Map<Hash, AccountsState> accountsState = accountsStateProvider.initialAccountsState();
    this.initialRootHash = accountsStateProvider.lastAcceptedRootHash();
    AccountRepository accountRepository = new AccountRepositoryImp(accountsState);
    this.accountService = new AccountServiceImp(accountRepository, initialRootHash);
  }

  @Test
  public void innerRollupTransfer() {
    List<RTransfer> transfers = innerRollupTransfers();
    List<RTransfer> invalidTransfers =
        accountService.updateIfAllTransfersValid(transfers, initialRootHash);
    assertTrue(invalidTransfers.isEmpty());
    Hash updatedRootHash = accountService.getLastAcceptedRootHash();

    BigInteger aliceAmount = accountService.getAccount(updatedRootHash, alice.hash).amount;
    BigInteger bobAmount = accountService.getAccount(updatedRootHash, bob.hash).amount;
    assertEquals(aliceAmount, BigInteger.valueOf(90));
    assertEquals(bobAmount, BigInteger.valueOf(110));

    // Test rollback
    BigInteger initialAliceAmount = accountService.getAccount(initialRootHash, alice.hash).amount;
    BigInteger initialBobAmount = accountService.getAccount(initialRootHash, bob.hash).amount;

    assertEquals(initialAliceAmount, BigInteger.valueOf(100));
    assertEquals(initialBobAmount, BigInteger.valueOf(100));
  }

  @Test
  public void invalidBalanceInnerRollupTransfer() {
    List<RTransfer> transfers = invalidBalanceInnerRollupTransfers();
    List<RTransfer> invalidTransfers =
        accountService.updateIfAllTransfersValid(transfers, initialRootHash);
    assertFalse(invalidTransfers.isEmpty());
    Hash updatedRootHash = accountService.getLastAcceptedRootHash();

    BigInteger aliceAmount = accountService.getAccount(updatedRootHash, alice.hash).amount;
    BigInteger bobAmount = accountService.getAccount(updatedRootHash, bob.hash).amount;

    assertEquals(aliceAmount, BigInteger.valueOf(100));
    assertEquals(bobAmount, BigInteger.valueOf(100));
  }

  @Test
  public void crossRollupTransferShouldBeLocked() {
    short ridFrom = 1;
    short ridTo = 0;
    List<RTransfer> transfers = new ArrayList<>();
    RTransfer transfer1 =
        new RTransfer(
            1,
            alice,
            kate,
            BigInteger.valueOf(20),
            ridFrom,
            ridTo,
            new Signature(),
            100,
            Optional.empty());
    RTransfer transfer2 =
        new RTransfer(
            2,
            zac,
            kate,
            BigInteger.valueOf(30),
            ridFrom,
            ridTo,
            new Signature(),
            100,
            Optional.empty());

    transfers.add(transfer1);
    transfers.add(transfer2);

    accountService.updateIfAllTransfersValid(transfers, initialRootHash);
    Hash updatedRootHash = accountService.getLastAcceptedRootHash();
    BigInteger aliceAmount = accountService.getAccount(updatedRootHash, alice.hash).amount;
    BigInteger zacAmount = accountService.getAccount(updatedRootHash, zac.hash).amount;

    assertEquals(aliceAmount, BigInteger.valueOf(80));
    assertEquals(zacAmount, BigInteger.valueOf(70));

    Account transfer1Acc = accountService.getAccount(updatedRootHash, transfer1.hash);
    assertEquals(transfer1Acc.amount, BigInteger.valueOf(20));

    Account transfer2Acc = accountService.getAccount(updatedRootHash, transfer2.hash);
    assertEquals(transfer2Acc.amount, BigInteger.valueOf(30));
  }

  @Test
  public void checkBasicValidity() {
    List<RTransfer> invalidTransfers = invalidAccountInnerRollupTransfers();
    invalidTransfers.forEach(
        t -> {
          boolean isValid = accountService.checkBasicValidity(t, initialRootHash);
          assertFalse(isValid);
        });

    List<RTransfer> validTransfers = innerRollupTransfers();
    validTransfers.forEach(
        t -> {
          boolean isValid = accountService.checkBasicValidity(t, initialRootHash);
          assertTrue(isValid);
        });
  }

  private List<RTransfer> innerRollupTransfers() {
    short rid = 0;
    List<RTransfer> transfers = new ArrayList<>();
    RTransfer transfer =
        new RTransfer(
            1,
            alice,
            bob,
            BigInteger.valueOf(10),
            rid,
            rid,
            new Signature(),
            100,
            Optional.empty());
    transfers.add(transfer);
    return transfers;
  }

  private List<RTransfer> invalidBalanceInnerRollupTransfers() {
    short rid = 0;
    List<RTransfer> transfers = new ArrayList<>();
    RTransfer transfer =
        new RTransfer(
            0,
            alice,
            bob,
            BigInteger.valueOf(1000),
            rid,
            rid,
            new Signature(),
            100,
            Optional.empty());

    transfers.add(transfer);
    return transfers;
  }

  private List<RTransfer> invalidAccountInnerRollupTransfers() {
    short rid = 0;

    List<RTransfer> transfers = new ArrayList<>();
    RTransfer transfer =
        new RTransfer(
            0,
            kate,
            bob,
            BigInteger.valueOf(1000),
            rid,
            rid,
            new Signature(),
            100,
            Optional.empty());

    transfers.add(transfer);
    return transfers;
  }
}
