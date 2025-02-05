package live.thought.thoughtj.evolution;

import com.google.common.base.Preconditions;

import live.thought.thoughtj.core.*;
import live.thought.thoughtj.utils.Pair;
import live.thought.thoughtj.utils.Threading;

import static live.thought.thoughtj.core.Sha256Hash.hashTwice;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class SimplifiedMasternodeList extends Message {

    private ReentrantLock lock = Threading.lock("SimplifiedMasternodeList");

    private Sha256Hash blockHash;
    private long height;
    HashMap<Sha256Hash, SimplifiedMasternodeListEntry> mnMap;
    HashMap<Sha256Hash, Pair<Sha256Hash, Integer>> mnUniquePropertyMap;

    SimplifiedMasternodeList(NetworkParameters params) {
        super(params);
        blockHash = Sha256Hash.ZERO_HASH;
        height = -1;
        mnMap = new HashMap<Sha256Hash, SimplifiedMasternodeListEntry>(5000);
        mnUniquePropertyMap = new HashMap<Sha256Hash, Pair<Sha256Hash, Integer>>(5000);
    }

    SimplifiedMasternodeList(NetworkParameters params, byte [] payload, int offset) {
        super(params, payload, offset);
    }

    SimplifiedMasternodeList(SimplifiedMasternodeList other) {
        super(other.params);
        this.blockHash = other.blockHash;
        this.height = other.height;
        mnMap = new HashMap<Sha256Hash, SimplifiedMasternodeListEntry>(other.mnMap);
        mnUniquePropertyMap = new HashMap<Sha256Hash, Pair<Sha256Hash, Integer>>(other.mnUniquePropertyMap);
    }

    SimplifiedMasternodeList(NetworkParameters params, ArrayList<SimplifiedMasternodeListEntry> entries) {
        super(params);
        this.blockHash = Sha256Hash.ZERO_HASH;
        this.height = -1;
        mnUniquePropertyMap = new HashMap<Sha256Hash, Pair<Sha256Hash, Integer>>();
        mnMap = new HashMap<Sha256Hash, SimplifiedMasternodeListEntry>(entries.size());
        for(SimplifiedMasternodeListEntry entry : entries)
            addMN(entry);

    }

    @Override
    protected void parse() throws ProtocolException {
        blockHash = readHash();
        height = (int)readUint32();
        int size = (int)readVarInt();
        mnMap = new HashMap<Sha256Hash, SimplifiedMasternodeListEntry>(size);
        for(int i = 0; i < size; ++i)
        {
            Sha256Hash hash = readHash();
            SimplifiedMasternodeListEntry mn = new SimplifiedMasternodeListEntry(params, payload, cursor);
            cursor += mn.getMessageSize();
            mnMap.put(hash, mn);
        }

        size = (int)readVarInt();
        mnUniquePropertyMap = new HashMap<Sha256Hash, Pair<Sha256Hash, Integer>>(size);
        for(long i = 0; i < size; ++i)
        {
            Sha256Hash hash = readHash();
            Sha256Hash first = readHash();
            int second = (int)readUint32();
            mnUniquePropertyMap.put(hash, new Pair<Sha256Hash, Integer>(first, second));
        }
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(blockHash.getReversedBytes());
        Utils.uint32ToByteStreamLE(height, stream);

        stream.write(new VarInt(mnMap.size()).encode());
        for(Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet()) {
            stream.write(entry.getKey().getReversedBytes());
            entry.getValue().bitcoinSerializeToStream(stream);
        }
        stream.write(new VarInt(mnUniquePropertyMap.size()).encode());
        for(Map.Entry<Sha256Hash, Pair<Sha256Hash, Integer>> entry : mnUniquePropertyMap.entrySet()) {
            stream.write(entry.getKey().getReversedBytes());
            stream.write(entry.getValue().getFirst().getReversedBytes());
            Utils.uint32ToByteStreamLE(entry.getValue().getSecond().intValue(), stream);
        }
    }

    public int size() {
        return mnMap.size();
    }

    @Override
    public String toString() {
        return "Simplified MN List:  count:  " + size();
    }

    SimplifiedMasternodeList applyDiff(SimplifiedMasternodeListDiff diff)
    {
        CoinbaseTx cbtx = (CoinbaseTx)diff.coinBaseTx.getExtraPayloadObject();
        Preconditions.checkArgument(diff.prevBlockHash.equals(blockHash), "The mnlistdiff does not connect to this list.  height: " + height + " vs " + cbtx.getHeight());

        lock.lock();
        try {
            SimplifiedMasternodeList result;
            if(diff.hasChanges()) {
                //Since there are changes, make a copy of this list
                result = new SimplifiedMasternodeList(this);

                result.blockHash = diff.blockHash;
                result.height = cbtx.getHeight();

                for (Sha256Hash hash : diff.deletedMNs) {
                    result.removeMN(hash);
                }
                for (SimplifiedMasternodeListEntry entry : diff.mnList) {
                    result.addMN(entry);
                }
                return result;
            } else {
                //since there are no changes, modify this
                this.blockHash = diff.blockHash;
                this.height = cbtx.getHeight();
                return this;
            }

        } finally {
            lock.unlock();
        }
    }

    void addMN(SimplifiedMasternodeListEntry dmn)
    {
        lock.lock();
        try {
            mnMap.put(dmn.proRegTxHash, dmn);
            addUniqueProperty(dmn, dmn.service);
            addUniqueProperty(dmn, dmn.keyIdVoting);
            if (params.isSupportingEvolution())
                addUniqueProperty(dmn, dmn.pubKeyOperator);
            else
                addUniqueProperty(dmn, dmn.keyIdOperator);
        } finally {
            lock.unlock();
        }
    }

    void removeMN(Sha256Hash proTxHash) {
        lock.lock();
        try {
            SimplifiedMasternodeListEntry dmn = getMN(proTxHash);
            if (dmn != null) {
                deleteUniqueProperty(dmn, dmn.service);
                deleteUniqueProperty(dmn, dmn.keyIdVoting);
                deleteUniqueProperty(dmn, dmn.pubKeyOperator);
                mnMap.remove(proTxHash);
            }
        } finally {
            lock.unlock();
        }
    }

    public SimplifiedMasternodeListEntry getMN(Sha256Hash proTxHash)
    {
        lock.lock();
        try {
            SimplifiedMasternodeListEntry p = mnMap.get(proTxHash);
            if (p == null) {
                return null;
            }
            return p;
        } finally {
            lock.unlock();
        }
    }


    <T extends ChildMessage> void addUniqueProperty(SimplifiedMasternodeListEntry dmn, T value)
    {
        lock.lock();
        try {
            Sha256Hash hash = value.getHash();
            int i = 1;
            Pair<Sha256Hash, Integer> oldEntry = mnUniquePropertyMap.get(hash);
            //assert(oldEntry == null || oldEntry.getFirst().equals(dmn.proRegTxHash));
            if (oldEntry != null)
                i = oldEntry.getSecond() + 1;
            Pair<Sha256Hash, Integer> newEntry = new Pair(dmn.proRegTxHash, i);

            mnUniquePropertyMap.put(hash, newEntry);
        } finally {
            lock.unlock();
        }
    }
    <T extends ChildMessage>
    void deleteUniqueProperty(SimplifiedMasternodeListEntry dmn, T oldValue)
    {
        lock.lock();
        try {
            Sha256Hash oldHash = oldValue.getHash();
            Pair<Sha256Hash, Integer> p = mnUniquePropertyMap.get(oldHash);
            //assert(p != null && p.getFirst() == dmn.proRegTxHash);
            if (p.getSecond() == 1) {
                mnUniquePropertyMap.remove(oldHash);
            } else {
                mnUniquePropertyMap.put(oldHash, new Pair<Sha256Hash, Integer>(dmn.proRegTxHash, p.getSecond() - 1));
            }
        } finally {
            lock.unlock();
        }
    }


    boolean verify(Transaction coinbaseTx) throws VerificationException {
        //check mnListMerkleRoot

        if(!(coinbaseTx.getExtraPayloadObject() instanceof CoinbaseTx))
            throw new VerificationException("transaction is not a coinbase transaction");

        CoinbaseTx cbtx = (CoinbaseTx)coinbaseTx.getExtraPayloadObject();

        lock.lock();
        try {
            ArrayList<Sha256Hash> proTxHashes = new ArrayList<Sha256Hash>(mnMap.size());
            for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet()) {
                proTxHashes.add(entry.getValue().proRegTxHash);
            }
            Collections.sort(proTxHashes, new Comparator<Sha256Hash>() {
                @Override
                public int compare(Sha256Hash o1, Sha256Hash o2) {
                    return o1.compareTo(o2);
                }
            });

            ArrayList<Sha256Hash> smnlHashes = new ArrayList<Sha256Hash>(mnMap.size());
            for (Sha256Hash hash : proTxHashes) {
                for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet())
                    if (entry.getValue().proRegTxHash.equals(hash))
                        smnlHashes.add(entry.getValue().getHash());
            }

            if (smnlHashes.size() == 0)
                return true;

            if (!cbtx.merkleRootMasternodeList.equals(calculateMerkleRoot(smnlHashes)))
                throw new VerificationException("MerkleRoot of masternode list does not match coinbaseTx");
            return true;
        } finally {
            lock.unlock();
        }
    }

    public Sha256Hash calculateMerkleRoot() {
        lock.lock();
        try {
            ArrayList<Sha256Hash> proTxHashes = new ArrayList<Sha256Hash>(mnMap.size());
            for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet()) {
                proTxHashes.add(entry.getValue().proRegTxHash);
            }

            Collections.sort(proTxHashes, new Comparator<Sha256Hash>() {
                @Override
                public int compare(Sha256Hash o1, Sha256Hash o2) {
                    return o1.compareTo(o2);
                }
            });
            ArrayList<Sha256Hash> smnlHashes = new ArrayList<Sha256Hash>(mnMap.size());
            for (Sha256Hash hash : proTxHashes) {
                for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet())
                    if (entry.getValue().proRegTxHash.equals(hash))
                        smnlHashes.add(entry.getValue().getHash());
            }

            return calculateMerkleRoot(smnlHashes);
        } finally {
            lock.unlock();
        }
    }

    private Sha256Hash calculateMerkleRoot(List<Sha256Hash> hashes) {
        List<byte[]> tree = buildMerkleTree(hashes);
        return Sha256Hash.wrap(tree.get(tree.size() - 1));
    }

    private List<byte[]> buildMerkleTree(List<Sha256Hash> hashes) {
        // The Merkle root is based on a tree of hashes calculated from the masternode list proRegHash:
        //
        //     root
        //      / \
        //   A      B
        //  / \    / \
        // t1 t2 t3 t4
        //
        // The tree is represented as a list: t1,t2,t3,t4,A,B,root where each
        // entry is a hash.
        //
        // The hashing algorithm is double SHA-256. The leaves are a hash of the serialized contents of the transaction.
        // The interior nodes are hashes of the concenation of the two child hashes.
        //
        // This structure allows the creation of proof that a transaction was included into a block without having to
        // provide the full block contents. Instead, you can provide only a Merkle branch. For example to prove tx2 was
        // in a block you can just provide tx2, the hash(tx1) and B. Now the other party has everything they need to
        // derive the root, which can be checked against the block header. These proofs aren't used right now but
        // will be helpful later when we want to download partial block contents.
        //
        // Note that if the number of transactions is not even the last tx is repeated to make it so (see
        // tx3 above). A tree with 5 transactions would look like this:
        //
        //         root
        //        /     \
        //       1        5
        //     /   \     / \
        //    2     3    4  4
        //  / \   / \   / \
        // t1 t2 t3 t4 t5 t5
        ArrayList<byte[]> tree = new ArrayList<byte[]>();
        // Start by adding all the hashes of the transactions as leaves of the tree.
        for (Sha256Hash hash : hashes) {
            tree.add(hash.getBytes());
        }
        int levelOffset = 0; // Offset in the list where the currently processed level starts.
        // Step through each level, stopping when we reach the root (levelSize == 1).
        for (int levelSize = hashes.size(); levelSize > 1; levelSize = (levelSize + 1) / 2) {
            // For each pair of nodes on that level:
            for (int left = 0; left < levelSize; left += 2) {
                // The right hand node can be the same as the left hand, in the case where we don't have enough
                // transactions.
                int right = Math.min(left + 1, levelSize - 1);
                byte[] leftBytes = Utils.reverseBytes(tree.get(levelOffset + left));
                byte[] rightBytes = Utils.reverseBytes(tree.get(levelOffset + right));
                tree.add(Utils.reverseBytes(hashTwice(leftBytes, 0, 32, rightBytes, 0, 32)));
            }
            // Move to the next level.
            levelOffset += levelSize;
        }
        return tree;
    }

    public interface ForeachMNCallback {
        void processMN(SimplifiedMasternodeListEntry mn);
    }

    public void forEachMN(boolean onlyValid, ForeachMNCallback callback) {
        lock.lock();
        try {
            for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> entry : mnMap.entrySet()) {
                if (!onlyValid || isMNValid(entry.getValue())) {
                    callback.processMN(entry.getValue());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public int getAllMNsCount()
    {
        return mnMap.size();
    }

    public int getValidMNsCount()
    {
        lock.lock();
        try {
            int count = 0;
            for (Map.Entry<Sha256Hash, SimplifiedMasternodeListEntry> p : mnMap.entrySet()) {
                if (isMNValid(p.getValue())) {
                    count++;
                }
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    public boolean isMNValid(SimplifiedMasternodeListEntry entry) {
        return entry.isValid;
    }

    ArrayList<Pair<Sha256Hash, SimplifiedMasternodeListEntry>> calculateScores(final Sha256Hash modifier)
    {
        final ArrayList<Pair<Sha256Hash, SimplifiedMasternodeListEntry>> scores = new ArrayList<Pair<Sha256Hash, SimplifiedMasternodeListEntry>>(getAllMNsCount());

        forEachMN(true, new ForeachMNCallback() {
            @Override
            public void processMN(SimplifiedMasternodeListEntry mn) {
                if(mn.getConfirmedHash().equals(Sha256Hash.ZERO_HASH)) {
                    // we only take confirmed MNs into account to avoid hash grinding on the ProRegTxHash to sneak MNs into a
                    // future quorums
                    return;
                }

                // calculate sha256(sha256(proTxHash, confirmedHash), modifier) per MN
                // Please note that this is not a double-sha256 but a single-sha256
                // The first part is already precalculated (confirmedHashWithProRegTxHash)
                // TODO When https://github.com/bitcoin/bitcoin/pull/13191 gets backported, implement something that is similar but for single-sha256
                try {
                    UnsafeByteArrayOutputStream bos = new UnsafeByteArrayOutputStream(64);
                    bos.write(mn.getConfirmedHashWithProRegTxHash().getReversedBytes());
                    bos.write(modifier.getReversedBytes());
                    scores.add(new Pair(Sha256Hash.of(bos.toByteArray()), mn)); //we don't reverse this, it is not for a wire message
                } catch (IOException x) {
                    throw new RuntimeException(x);
                }
            }
        });

        return scores;
    }

    class CompareScoreMN<Object> implements Comparator<Object>
    {
        public int compare(Object t1, Object t2) {
            Pair<Sha256Hash, SimplifiedMasternodeListEntry> p1 = (Pair<Sha256Hash, SimplifiedMasternodeListEntry>)t1;
            Pair<Sha256Hash, SimplifiedMasternodeListEntry> p2 = (Pair<Sha256Hash, SimplifiedMasternodeListEntry>)t2;

            if(p1.getFirst().compareTo(p2.getFirst()) < 0)
                return -1;
            if(p1.getFirst().equals(p2.getFirst()))
                return 0;
            else return 1;
        }
    }

    public int getMasternodeRank(Sha256Hash proTxHash, Sha256Hash quorumModifierHash)
    {
        int rank = -1;
        //Added to speed things up

        SimplifiedMasternodeListEntry mnExisting = getMN(proTxHash);
        if (mnExisting == null)
            return -1;

        //lock.lock();
        try {

            ArrayList<Pair<Sha256Hash, SimplifiedMasternodeListEntry>> vecMasternodeScores = calculateScores(quorumModifierHash);
            if (vecMasternodeScores.isEmpty())
                return -1;

            Collections.sort(vecMasternodeScores, Collections.reverseOrder(new CompareScoreMN()));


            rank = 0;
            for (Pair<Sha256Hash, SimplifiedMasternodeListEntry> scorePair : vecMasternodeScores) {
                rank++;
                if (scorePair.getSecond().getProRegTxHash().equals(proTxHash)) {
                    return rank;
                }
            }
            return -1;
        } finally {
            //lock.unlock();
        }
    }
}
