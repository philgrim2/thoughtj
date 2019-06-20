package live.thought.thoughtj.evolution;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import live.thought.thoughtj.core.*;
import live.thought.thoughtj.evolution.SubTxResetKey;
import live.thought.thoughtj.params.UnitTestParams;
import live.thought.thoughtj.script.ScriptBuilder;

import java.io.IOException;

import static live.thought.thoughtj.script.ScriptOpCodes.OP_RETURN;
import static org.junit.Assert.*;

public class SubTxResetKeyTest {

    Context context;
    UnitTestParams PARAMS;
    byte[] txdata;

    @Before
    public void startup() {
        PARAMS = UnitTestParams.get();
        context = Context.getOrCreate(PARAMS);
        txdata = Utils.HEX.decode("03000a00000000000000a00100659c3243efcab7813a06664582300960844dc291988b1510afac99efa001370d659c3243efcab7813a06664582300960844dc291988b1510afac99efa001370de803000000000000f6f5abf4ba75c554b9ef001a78c35ce5edb3ccb1411fd442ee3bb6dac571f432e56def3d06f64a15cc74f382184ca4d5d4cad781ced01ae4e8109411f548da5c5fa6bfce5a23a8d620104e6953600539728b95077e19");         //"01000873616d697366756ec3bfec8ca49279bb1375ad3461f654ff1a277d464120f19af9563ef387fef19c82bc4027152ef5642fe8158ffeb3b8a411d9a967b6af0104b95659106c8a9d7451478010abe042e58afc9cdaf006f77cab16edcb6f84";
    }

    @Test
    public void verifyTest() {
        Sha256Hash txId = Sha256Hash.wrap("251961000a115bafbb7bdb6e1baf23d88e37ecf2fe6af5d9572884cabaecdcc0");
        Sha256Hash blockchainUserRegistrationTransactionHash = Sha256Hash.wrap("0d3701a0ef99acaf10158b9891c24d84600930824566063a81b7caef43329c65");
        Sha256Hash blockchainUserPreviousTransactionHash = Sha256Hash.wrap("0d3701a0ef99acaf10158b9891c24d84600930824566063a81b7caef43329c65");

        ECKey payloadKey = DumpedPrivateKey.fromBase58(PARAMS,"cVxAzue29NemggDqJyUwMsZ7KJsm4y9ntoW5UeCaTfQdruH2BKQR").getKey();
        Address payloadAddress = Address.fromBase58(PARAMS, "yfguWspuwx7ceKthnqqDc8CiZGZGRN7eFp");
        assertEquals("Payload key does not match input address", payloadAddress, payloadKey.toAddress(PARAMS));

        ECKey replacementPayloadKey = DumpedPrivateKey.fromBase58(PARAMS,"cPG7GuByFnYkGvkrZqw8chGNfJYmKYnXt6TBjHruaApC42CPwwTE").getKey();
        Address replacementPayloadAddress = Address.fromBase58(PARAMS, "yiqFNxn9kbWEKj7B87aEnoyChBL8rMFymt");
        KeyId replacementPubkeyHash = new KeyId(Utils.reverseBytes(Utils.HEX.decode("b1ccb3ede55cc3781a00efb954c575baf4abf5f6")));


        Transaction tx = new Transaction(PARAMS, txdata);
        SubTxResetKey resetKey = (SubTxResetKey)tx.getExtraPayloadObject();
        assertEquals(txId, tx.getHash());
        assertEquals(replacementPubkeyHash, resetKey.newPubKeyId);
        assertEquals(blockchainUserRegistrationTransactionHash, resetKey.regTxId);
        assertEquals(blockchainUserPreviousTransactionHash, resetKey.hashPrevSubTx);
        try {
            UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(tx.getMessageSize());
            tx.bitcoinSerialize(bos);
            assertArrayEquals("Blockchain user reset transaction does not match it's data", txdata, bos.toByteArray());
        } catch (IOException x) {
            fail(x.getMessage());
        }
    }

    @Test
    public void createSubTxResetKey() {
        Sha256Hash txId = Sha256Hash.wrap("251961000a115bafbb7bdb6e1baf23d88e37ecf2fe6af5d9572884cabaecdcc0");
        Sha256Hash blockchainUserRegistrationTransactionHash = Sha256Hash.wrap("0d3701a0ef99acaf10158b9891c24d84600930824566063a81b7caef43329c65");
        Sha256Hash blockchainUserPreviousTransactionHash = Sha256Hash.wrap("0d3701a0ef99acaf10158b9891c24d84600930824566063a81b7caef43329c65");

        ECKey payloadKey = DumpedPrivateKey.fromBase58(PARAMS,"cVxAzue29NemggDqJyUwMsZ7KJsm4y9ntoW5UeCaTfQdruH2BKQR").getKey();
        Address payloadAddress = Address.fromBase58(PARAMS, "yfguWspuwx7ceKthnqqDc8CiZGZGRN7eFp");
        assertEquals("Payload key does not match input address", payloadAddress, payloadKey.toAddress(PARAMS));

        ECKey replacementPayloadKey = DumpedPrivateKey.fromBase58(PARAMS,"cPG7GuByFnYkGvkrZqw8chGNfJYmKYnXt6TBjHruaApC42CPwwTE").getKey();
        Address replacementPayloadAddress = Address.fromBase58(PARAMS, "yiqFNxn9kbWEKj7B87aEnoyChBL8rMFymt");
        KeyId replacementPubkeyId = new KeyId(Utils.reverseBytes(Utils.HEX.decode("b1ccb3ede55cc3781a00efb954c575baf4abf5f6")));
        assertArrayEquals(replacementPubkeyId.getBytes(), replacementPayloadKey.getPubKeyHash());

        Transaction tx = new Transaction(PARAMS);
        tx.setVersion(3);
        tx.setType(Transaction.Type.TRANSACTION_SUBTX_RESETKEY);

        SubTxResetKey resetKey = new SubTxResetKey(1, blockchainUserRegistrationTransactionHash,
                blockchainUserPreviousTransactionHash, Coin.valueOf(1000), replacementPubkeyId, payloadKey);

        tx.setExtraPayload(resetKey);
        byte [] payloadDataToConfirm = Utils.HEX.decode("0100659c3243efcab7813a06664582300960844dc291988b1510afac99efa001370d659c3243efcab7813a06664582300960844dc291988b1510afac99efa001370de803000000000000f6f5abf4ba75c554b9ef001a78c35ce5edb3ccb1411fd442ee3bb6dac571f432e56def3d06f64a15cc74f382184ca4d5d4cad781ced01ae4e8109411f548da5c5fa6bfce5a23a8d620104e6953600539728b95077e19");
        assertArrayEquals("Payload Data does not match, signing payload does not work", payloadDataToConfirm, resetKey.getPayload());

         assertEquals(txId, tx.getHash());
        try {
            UnsafeByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(txdata.length);
            tx.bitcoinSerialize(stream);
            assertArrayEquals("resetkey transaction does not match it's data", txdata, stream.toByteArray());
        } catch (IOException x) {
            fail();
        }
    }
}
