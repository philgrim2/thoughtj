/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package live.thought.thoughtj.core;

import com.google.common.collect.ImmutableList;

import live.thought.thoughtj.core.AbstractBlockChain;
import live.thought.thoughtj.core.Address;
import live.thought.thoughtj.core.Coin;
import live.thought.thoughtj.core.ECKey;
import live.thought.thoughtj.core.Transaction;
import live.thought.thoughtj.core.TransactionOutput;
import live.thought.thoughtj.params.MainNetParams;
import live.thought.thoughtj.script.Script;
import live.thought.thoughtj.script.ScriptBuilder;
import live.thought.thoughtj.testing.TestWithWallet;
import live.thought.thoughtj.wallet.SendRequest;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionOutputTest extends TestWithWallet {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMultiSigOutputToString() throws Exception {
        sendMoneyToWallet(AbstractBlockChain.NewBlockType.BEST_CHAIN, Coin.COIN);
        ECKey myKey = new ECKey();
        this.wallet.importKey(myKey);

        // Simulate another signatory
        ECKey otherKey = new ECKey();

        // Create multi-sig transaction
        Transaction multiSigTransaction = new Transaction(PARAMS);
        ImmutableList<ECKey> keys = ImmutableList.of(myKey, otherKey);

        Script scriptPubKey = ScriptBuilder.createMultiSigOutputScript(2, keys);
        multiSigTransaction.addOutput(Coin.COIN, scriptPubKey);

        SendRequest req = SendRequest.forTx(multiSigTransaction);
        this.wallet.completeTx(req);
        TransactionOutput multiSigTransactionOutput = multiSigTransaction.getOutput(0);

        assertThat(multiSigTransactionOutput.toString(), CoreMatchers.containsString("CHECKMULTISIG"));
    }

    @Test
    public void testP2SHOutputScript() throws Exception {
        String P2SHAddressString = "7WJnm5FSpJttSr72bWWqFFZrXwB8ZzsK7b";
        Address P2SHAddress = Address.fromBase58(MainNetParams.get(), P2SHAddressString);
        Script script = ScriptBuilder.createOutputScript(P2SHAddress);
        Transaction tx = new Transaction(MainNetParams.get());
        tx.addOutput(Coin.COIN, script);
        assertEquals(P2SHAddressString, tx.getOutput(0).getAddressFromP2SH(MainNetParams.get()).toString());
    }

    @Test
    public void getAddressTests() throws Exception {
        Transaction tx = new Transaction(MainNetParams.get());
        tx.addOutput(Coin.CENT, ScriptBuilder.createOpReturnScript("hello world!".getBytes()));
        assertNull(tx.getOutput(0).getAddressFromP2SH(PARAMS));
        assertNull(tx.getOutput(0).getAddressFromP2PKHScript(PARAMS));
    }

    @Test
    public void getMinNonDustValue() throws Exception {
        TransactionOutput payToAddressOutput = new TransactionOutput(PARAMS, null, Coin.COIN, myAddress);
        assertEquals(Transaction.MIN_NONDUST_OUTPUT, payToAddressOutput.getMinNonDustValue());
    }
}
