package ec.edu.espe.buildtestci.model;

import java.util.UUID;

public class Wallet {

    private final String id;
    private final String ownerEmail;
    private double balance;

    public Wallet(String ownerEmail, double initialBalance) {
        this.id = UUID.randomUUID().toString();
        this.ownerEmail = ownerEmail;
        this.balance = initialBalance;
    }

    public String getId() {
        return id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public double getBalance() {
        return balance;
    }

    //Depositar dinero en la cuenta
    public void deposit(double amount) {
        this.balance += amount;
    }

    //Retirar dinero si existe saldo suficiente
    public void withdraw(double amount) {
        this.balance -= amount;
    }
}
