/*
 * Copyright 2015 Ross Nicoll.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package live.thought.thoughtj.examples;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.EnumSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static live.thought.thoughtj.script.ScriptOpCodes.getOpCodeName;

import live.thought.thoughtj.core.Coin;
import live.thought.thoughtj.core.ECKey;
import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.core.ScriptException;
import live.thought.thoughtj.core.Transaction;
import live.thought.thoughtj.core.TransactionInput;
import live.thought.thoughtj.core.TransactionOutput;
import live.thought.thoughtj.core.Utils;
import live.thought.thoughtj.crypto.TransactionSignature;
import live.thought.thoughtj.params.MainNetParams;
import live.thought.thoughtj.script.Script;
import live.thought.thoughtj.script.ScriptBuilder;
import live.thought.thoughtj.script.ScriptChunk;
import live.thought.thoughtj.signers.LocalTransactionSigner;
import live.thought.thoughtj.signers.TransactionSigner.ProposedTransaction;
import live.thought.thoughtj.wallet.KeyBag;
import live.thought.thoughtj.wallet.RedeemData;

/**
 * Test case generator for transactions with low-S and high-S signatures, to
 * test the LOW_S script validation flag.
 *
 * @author Ross Nicoll
 */
public class GenerateLowSTests {
    public static final BigInteger HIGH_S_DIFFERENCE = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    public static void main(final String[] argv) throws NoSuchAlgorithmException, IOException {
        final NetworkParameters params = new MainNetParams();
        final LocalTransactionSigner signer = new LocalTransactionSigner();
        final SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        final ECKey key = new ECKey(secureRandom);
        final KeyBag bag = new KeyBag() {
            @Override
            public ECKey findKeyFromPubHash(byte[] pubkeyHash) {
                return key;
            }

            @Override
            public ECKey findKeyFromPubKey(byte[] pubkey) {
                return key;
            }

            @Override
            public RedeemData findRedeemDataFromScriptHash(byte[] scriptHash) {
                return null;
            }

        };

        // Generate a fictional output transaction we take values from, and
        // an input transaction for the test case

        final Transaction outputTransaction = new Transaction(params);
        final Transaction inputTransaction = new Transaction(params);
        final TransactionOutput output = new TransactionOutput(params, inputTransaction, Coin.ZERO, key.toAddress(params));

        inputTransaction.addOutput(output);
        outputTransaction.addInput(output);
        outputTransaction.addOutput(Coin.ZERO, new ECKey(secureRandom).toAddress(params));

        addOutputs(outputTransaction, bag);

        // Sign the transaction
        final ProposedTransaction proposedTransaction = new ProposedTransaction(outputTransaction);
        signer.signInputs(proposedTransaction, bag);
        final TransactionInput input = proposedTransaction.partialTx.getInput(0);

        input.verify(output);
        input.getScriptSig().correctlySpends(outputTransaction, 0, output.getScriptPubKey(),
            EnumSet.of(Script.VerifyFlag.DERSIG, Script.VerifyFlag.P2SH));

        final Script scriptSig = input.getScriptSig();
        final TransactionSignature signature = TransactionSignature.decodeFromBitcoin(scriptSig.getChunks().get(0).data, true, false);

        // First output a conventional low-S transaction with the LOW_S flag, for the tx_valid.json set
        System.out.println("[\"A transaction with a low-S signature.\"],");
        System.out.println("[[[\""
            + inputTransaction.getHashAsString() + "\", "
            + output.getIndex() + ", \""
            + scriptToString(output.getScriptPubKey()) + "\"]],\n"
            + "\"" + Utils.HEX.encode(proposedTransaction.partialTx.unsafeBitcoinSerialize()) + "\", \""
            + Script.VerifyFlag.P2SH.name() + "," + Script.VerifyFlag.LOW_S.name() + "\"],");

        final BigInteger highS = HIGH_S_DIFFERENCE.subtract(signature.s);
        final TransactionSignature highSig = new TransactionSignature(signature.r, highS);
        input.setScriptSig(new ScriptBuilder().data(highSig.encodeToBitcoin()).data(scriptSig.getChunks().get(1).data).build());
        input.getScriptSig().correctlySpends(outputTransaction, 0, output.getScriptPubKey(),
            EnumSet.of(Script.VerifyFlag.P2SH));

        // A high-S transaction without the LOW_S flag, for the tx_valid.json set
        System.out.println("[\"A transaction with a high-S signature.\"],");
        System.out.println("[[[\""
            + inputTransaction.getHashAsString() + "\", "
            + output.getIndex() + ", \""
            + scriptToString(output.getScriptPubKey()) + "\"]],\n"
            + "\"" + Utils.HEX.encode(proposedTransaction.partialTx.unsafeBitcoinSerialize()) + "\", \""
            + Script.VerifyFlag.P2SH.name() + "\"],");

        // Lastly a conventional high-S transaction with the LOW_S flag, for the tx_invalid.json set
        System.out.println("[\"A transaction with a high-S signature.\"],");
        System.out.println("[[[\""
            + inputTransaction.getHashAsString() + "\", "
            + output.getIndex() + ", \""
            + scriptToString(output.getScriptPubKey()) + "\"]],\n"
            + "\"" + Utils.HEX.encode(proposedTransaction.partialTx.unsafeBitcoinSerialize()) + "\", \""
            + Script.VerifyFlag.P2SH.name() + "," + Script.VerifyFlag.LOW_S.name() + "\"],");
    }

    private static void addOutputs(final Transaction outputTransaction, final KeyBag bag) throws ScriptException {
        int numInputs = outputTransaction.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = outputTransaction.getInput(i);
            Script scriptPubKey = txIn.getConnectedOutput().getScriptPubKey();
            RedeemData redeemData = txIn.getConnectedRedeemData(bag);
            checkNotNull(redeemData, "Transaction exists in wallet that we cannot redeem: %s", txIn.getOutpoint().getHash());
            txIn.setScriptSig(scriptPubKey.createEmptyInputScript(redeemData.keys.get(0), redeemData.redeemScript));
        }
    }

    /**
     * Convert a script to a string format that suits the style expected in
     * tx_valid.json and tx_invalid.json.
     */
    private static String scriptToString(Script scriptPubKey) {
        final StringBuilder buf = new StringBuilder();
        for (ScriptChunk chunk: scriptPubKey.getChunks()) {
            if (buf.length() > 0) {
                buf.append(" ");
            }
            if (chunk.isOpCode()) {
                buf.append(getOpCodeName(chunk.opcode));
            } else if (chunk.data != null) {
                // Data chunk
                buf.append("0x")
                    .append(Integer.toString(chunk.opcode, 16)).append(" 0x")
                    .append(Utils.HEX.encode(chunk.data));
            } else {
                buf.append(chunk.toString());
            }
        }
        return buf.toString();
    }
}
