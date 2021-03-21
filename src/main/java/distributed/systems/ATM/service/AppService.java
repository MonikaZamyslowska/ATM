package distributed.systems.ATM.service;

import distributed.systems.ATM.domain.BankAccount;
import distributed.systems.ATM.domain.BankTransaction;
import distributed.systems.ATM.domain.User;
import distributed.systems.ATM.exception.BankAccountNotFound;
import distributed.systems.ATM.exception.BankTransactionRejected;
import distributed.systems.ATM.repository.BankAccountRepository;
import distributed.systems.ATM.repository.BankTransactionRepository;
import distributed.systems.ATM.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AppService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Transactional
    public void processTransaction(BankTransaction bankTransaction) throws Exception {
        BankAccount senderBankAccount = bankTransaction.getSenderBankAccount();

        switch (bankTransaction.getTransactionType()) {
            case PAYOUT:
                doPayout(senderBankAccount, bankTransaction);
                bankTransactionRepository.save(bankTransaction);
                System.out.printf("payout");
                break;
            case PAYMENT:
                doPayment(senderBankAccount, bankTransaction);
                bankTransactionRepository.save(bankTransaction);
                System.out.printf("payment");
                break;
            case TRANSFER:
                doTransfer(bankTransaction);
                bankTransactionRepository.save(bankTransaction);
                System.out.printf("transfer");
                break;
            default:
                throw new BankTransactionRejected("Something went wrong. Please try again.");
        }
    }

    private void doTransfer(BankTransaction bankTransaction) throws BankAccountNotFound {
        BankAccount receiverAccount = getBankAccountByAccountNum(bankTransaction.getReceiverAccountNumber());
        doPayment(receiverAccount, bankTransaction);
        doPayout(getSenderBankAccount(bankTransaction), bankTransaction);
    }

    public void doPayment(BankAccount bankAccount, BankTransaction bankTransaction) {
        BigDecimal balance = bankAccount.getBalance();
        bankAccount.setBalance(balance.add(bankTransaction.getAmount()));
        bankAccountRepository.save(bankAccount);
    }

    private void doPayout(BankAccount bankAccount, BankTransaction bankTransaction) {
        BigDecimal balance = bankAccount.getBalance();
        bankAccount.setBalance(balance.subtract(bankTransaction.getAmount()));
        bankAccountRepository.save(bankAccount);
    }

    public User getOwnerUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findUserByEmail(email).orElseThrow();
    }

    public BankAccount getBankAccountByAccountNum(int accountNumber) throws BankAccountNotFound {
        return bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BankAccountNotFound("Bank account not found. Please try again"));
    }

    public BankAccount getSenderBankAccount(BankTransaction bankTransaction) {
        BankAccount bankAccount = bankTransaction.getSenderBankAccount();
        return bankAccount;
    }

    public BankTransaction createBankTransaction() {
        BankTransaction bankTransaction = new BankTransaction();
        User user = getOwnerUser();
        bankTransaction.setSenderBankAccount(user.getAccount());
        bankTransaction.setCreatedDate(LocalDateTime.now());
        return bankTransaction;
    }
}
