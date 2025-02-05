package live.thought.thoughtj.evolution;

import static live.thought.thoughtj.core.Sha256Hash.hashTwice;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import live.thought.thoughtj.core.*;

public class SimplifiedMasternodeListDiff extends Message {
    public Sha256Hash prevBlockHash;
    public Sha256Hash blockHash;
    PartialMerkleTree cbTxMerkleTree;
    Transaction coinBaseTx;
    protected HashSet<Sha256Hash> deletedMNs;
    protected ArrayList<SimplifiedMasternodeListEntry> mnList;


    public SimplifiedMasternodeListDiff(NetworkParameters params, byte [] payload) {
        super(params, payload, 0);
    }

    @Override
    protected void parse() throws ProtocolException {
        prevBlockHash = readHash();
        blockHash = readHash();

        cbTxMerkleTree = new PartialMerkleTree(params, payload, cursor);
        cursor += cbTxMerkleTree.getMessageSize();

        coinBaseTx = new Transaction(params, payload, cursor);
        cursor += coinBaseTx.getMessageSize();

        int size = (int)readVarInt();
        deletedMNs = new HashSet<Sha256Hash>(size);
        for(int i = 0; i < size; ++i) {
            deletedMNs.add(readHash());
        }

        size = (int)readVarInt();
        mnList = new ArrayList<SimplifiedMasternodeListEntry>(size);
        for(int i = 0; i < size; ++i)
        {
            SimplifiedMasternodeListEntry mn = new SimplifiedMasternodeListEntry(params, payload, cursor);
            cursor += mn.getMessageSize();
            mnList.add(mn);
        }
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(prevBlockHash.getReversedBytes());
        stream.write(blockHash.getReversedBytes());

        cbTxMerkleTree.bitcoinSerializeToStream(stream);
        coinBaseTx.bitcoinSerialize(stream);

        stream.write(new VarInt(deletedMNs.size()).encode());
        for(Sha256Hash entry : deletedMNs) {
            stream.write(entry.getReversedBytes());
        }

        stream.write(new VarInt(mnList.size()).encode());
        for(SimplifiedMasternodeListEntry entry : mnList) {
            entry.bitcoinSerializeToStream(stream);
        }
    }

    public boolean hasChanges() {
        return !mnList.isEmpty() || !deletedMNs.isEmpty();
    }

    boolean verify() {
        //check that coinbase is in the merkle root
        return true;
    }

    @Override
    public String toString() {
        return "Simplified MNList Diff:  adding " + mnList.size() + " and removing " + deletedMNs.size() + " masternodes";
    }
}
