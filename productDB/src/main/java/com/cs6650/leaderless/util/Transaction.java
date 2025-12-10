package com.cs6650.leaderless.util;

public class Transaction {

    public static void begin() {
        System.out.println("BEGIN TRANSACTION");
    }

    public static void commit() {
        System.out.println("COMMIT TRANSACTION");
    }

    public static void abort() {
        System.out.println("ABORT TRANSACTION");
    }
}
