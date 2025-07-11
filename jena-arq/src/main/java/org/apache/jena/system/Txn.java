/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.system;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Transactional;

/** Application utilities for executing code in transactions.
 * <p>
 * Nested transaction are not supported but calling inside an existing transaction,
 * which must be compatible, (i.e. a write needs a WRITE transaction).
 * causes the existing transaction to be used.
 */

public class Txn {
    /**
     * Execute in a "read" transaction that can promote to "write".
     * <p>
     * Such a transaction may abort if an update is executed
     * by another thread before this one is promoted to "write" mode.
     * If so, the data protected by {@code txn} is unchanged.
     * <p>
     * If the application knows updates will be needed, consider using {@link #executeWrite}
     * which starts in "write" mode.
     * <p>
     * The application code can call {@link Transactional#promote} to attempt to
     * change from "read" to "write"; the {@link Transactional#promote promote} method
     * returns a boolean indicating whether the promotion was possible or not.
     */
    public static <T extends Transactional> void execute(T txn, Runnable r) {
        exec(txn, TxnType.READ_PROMOTE, r);
    }

    /**
     * Execute in a "read" transaction that can promote to "write" and return some calculated value.
     * <p>
     * Such a transaction may abort if an update is executed
     * by another thread before this one is promoted to "write" mode.
     * If so, the data protected by {@code txn} is unchanged.
     * <p>
     * If the application knows updates will be needed, consider using {@link #executeWrite}
     * which starts in "write" mode.
     * <p>
     * The application code can call {@link Transactional#promote} to attempt to
     * change from "read" to "write"; the {@link Transactional#promote promote} method
     * returns a boolean indicating whether the promotion was possible or not.
     */
    public static <T extends Transactional, X> X calculate(T txn, Supplier<X> r) {
        return calc(txn, TxnType.READ_PROMOTE, r);
    }

    /** Execute application code in a transaction with the given {@link TxnType transaction type}. */
    public static <T extends Transactional> void exec(T txn, TxnType txnType, Runnable r) {
        boolean b = txn.isInTransaction();
        if ( b )
            TxnOp.compatibleWithPromote(txnType, txn);
        else
            txn.begin(txnType);
        try { r.run(); }
        catch (Throwable th) {
            onThrowable(th, txn);
            throw th;
        }
        if ( !b ) {
            if ( txn.isInTransaction() )
                // May have been explicit commit or abort.
                txn.commit();
            txn.end();
        }
    }

    /** Execute and return a value in a transaction with the given {@link TxnType transaction type}. */
    public static <T extends Transactional, X> X calc(T txn, TxnType txnType, Supplier<X> r) {
        boolean b = txn.isInTransaction();
        if ( b )
            TxnOp.compatibleWithPromote(txnType, txn);
        else
            txn.begin(txnType);
        X x;
        try { x = r.get(); }
        catch (Throwable th) {
            onThrowable(th, txn);
            throw th;
        }

        if ( !b ) {
            if ( txn.isInTransaction() )
                // May have been explicit commit or abort.
                txn.commit();
            txn.end();
        }
        return x;
    }

    /** Execute in a read transaction */
    public static <T extends Transactional> void executeRead(T txn, Runnable r) {
        exec(txn, TxnType.READ, r);
    }

    /** Execute and return a value in a read transaction */
    public static <T extends Transactional, X> X calculateRead(T txn, Supplier<X> r) {
        return calc(txn, TxnType.READ, r);
    }

    /** Execute the Runnable in a write transaction */
    public static <T extends Transactional> void executeWrite(T txn, Runnable r) {
        exec(txn, TxnType.WRITE, r);
    }

    /** Execute and return a value in a write transaction. */
    public static <T extends Transactional, X> X calculateWrite(T txn, Supplier<X> r) {
        return calc(txn, TxnType.WRITE, r);
    }

    // Attempt some kind of cleanup.
    private static <T extends Transactional> void onThrowable(Throwable th, T txn) {
        try {
            txn.abort();
            txn.end();
        } catch (Throwable th2) { th.addSuppressed(th2); }
    }

    /**
     * Begins a transaction and returns a transaction control instance suitable
     * for use with try-with-resources blocks.
     * See {@link #autoTxn(Transactional, TxnType)}.
     */
    public static AutoTxn autoTxn(Transactional txn, ReadWrite readWrite) {
        return autoTxn(txn, TxnType.convert(readWrite));
    }

    /**
     * Begins a transaction and returns a {@linkplain AutoTxn} instance suitable
     * for use with try-with-resources blocks.
     * This allows for raising checked exceptions in an idiomatic way.
     * Closing the AutoTxn instance will abort the transaction unless there has
     * been an explicit call to {@linkplain AutoTxn#commit()}.
     * <p>
     *
     * Usage example:
     * <pre>
     * public void myMethod() throws IOException {
     *   try (AutoTxn txn = Txn.autoTxn(dataset, TxnType.WRITE)) {
     *     // Do work.
     *     if (someError) {
     *         throw new IOException();
     *     }
     *     // Explicitly call commit on success.
     *     txn.commit();
     *   }
     * }
     * </pre>
     */
    public static AutoTxn autoTxn(Transactional txn, TxnType txnType) {
        Objects.requireNonNull(txn);
        Objects.requireNonNull(txnType);
        boolean isTxnStartedHere = !txn.isInTransaction();
        if ( !isTxnStartedHere )
            TxnOp.compatibleWithPromote(txnType, txn);
        else
            txn.begin(txnType);
        return new AutoTxn(txn, isTxnStartedHere);
    }
}
