package org.bitcoinj.evolution;

import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PreMessageReceivedEventListener;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.quorums.SimplifiedQuorumList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class SimplifiedMasternodeListManager extends AbstractManager {
    private static final Logger log = LoggerFactory.getLogger(MasternodeManager.class);
    private ReentrantLock lock = Threading.lock("SimplifiedMasternodeListManager");

    static final int DMN_FORMAT_VERSION = 1;
    static final int LLMQ_FORMAT_VERSION = 2;

    public static final int SNAPSHOT_LIST_PERIOD = 576; // once per day
    public static final int LISTS_CACHE_SIZE = 576;

    HashMap<Sha256Hash, SimplifiedMasternodeList> mnListsCache;
    ArrayList<StoredBlock> pendingRequests;

    Peer currentPeer;
    //The hash is the base block, the diff are the changes made from that block
    HashMap<Sha256Hash, SimplifiedMasternodeListDiff> mapListDiffs;
    boolean requestingFirstList;
    SimplifiedMasternodeList mnList;
    SimplifiedQuorumList quorumList;
    long tipHeight;
    Sha256Hash tipBlockHash;

    AbstractBlockChain blockChain;

    Sha256Hash lastRequestHash = Sha256Hash.ZERO_HASH;
    int lastRequestCount;

    public SimplifiedMasternodeListManager(Context context) {
        super(context);
        tipBlockHash = Sha256Hash.ZERO_HASH;
        mnList = new SimplifiedMasternodeList(context.getParams());
        quorumList = new SimplifiedQuorumList(context.getParams());
        mnListsCache = new HashMap<Sha256Hash, SimplifiedMasternodeList>();
        mapListDiffs = new HashMap<Sha256Hash, SimplifiedMasternodeListDiff>();
        requestingFirstList = false;
        pendingRequests = new ArrayList<StoredBlock>();
    }

    @Override
    public int calculateMessageSizeInBytes() {
        return 0;
    }

    @Override
    public AbstractManager createEmpty() {
        return new SimplifiedMasternodeListManager(Context.get());
    }

    @Override
    public void checkAndRemove() {

    }

    @Override
    public void clear() {

    }

    @Override
    protected void parse() throws ProtocolException {
        mnList = new SimplifiedMasternodeList(params, payload, cursor);
        cursor += mnList.getMessageSize();
        tipBlockHash = readHash();
        tipHeight = readUint32();
        if(getFormatVersion() >= 2) {
            quorumList = new SimplifiedQuorumList(params, payload, cursor);
            cursor += quorumList.getMessageSize();
        }
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        lock.lock();
        try {
            mnList.bitcoinSerialize(stream);
            stream.write(tipBlockHash.getReversedBytes());
            Utils.uint32ToByteStreamLE(tipHeight, stream);
            if(getFormatVersion() >= 2)
                quorumList.bitcoinSerialize(stream);
        } finally {
            lock.unlock();
        }
    }

    public void updatedBlockTip(StoredBlock tip) {
    }

    public void processMasternodeListDiffMessage(Peer peer, SimplifiedMasternodeListDiff mnlistdiff) {

        mapListDiffs.put(mnlistdiff.prevBlockHash, mnlistdiff);
        //if(!processMasternodeListDiffs(peer)) {
        //    peer.close();
        //}
        processMasternodeListDiff(mnlistdiff);

        if(!pendingRequests.isEmpty()) {
            currentPeer = peer;
            processNextMNListDiff();
            //StoredBlock nextBlock = pendingRequests.get(0);
            //requestMNListDiff(peer, nextBlock);
            //pendingRequests.remove(0);
        }

    }

    protected boolean processMasternodeListDiffs(Peer peer) {

        return false;
    }

    public void processMasternodeListDiff(SimplifiedMasternodeListDiff mnlistdiff) {
        if(mnlistdiff.coinBaseTx.getVersionShort() < 3)
            throw new ProtocolException("mnlistdiff coinbaseTx has wrong version < 3: " + mnlistdiff.coinBaseTx.getVersionShort());
        long newHeight = ((CoinbaseTx) mnlistdiff.coinBaseTx.getExtraPayloadObject()).getHeight();
        log.info("processing mnlistdiff between : " + tipHeight + " & " + newHeight + "; " + mnlistdiff);
        lock.lock();
        int version = ((CoinbaseTx) mnlistdiff.coinBaseTx.getExtraPayloadObject()).getVersion();

        try {
            StoredBlock block = blockChain.getBlockStore().get(mnlistdiff.blockHash);
            if(block.getHeight() != newHeight)
                throw new ProtocolException("mnlistdiff blockhash (height="+block.getHeight()+" doesn't match coinbase blockheight: " + newHeight);
            SimplifiedMasternodeList newMNList = mnList.applyDiff(mnlistdiff);
            newMNList.verify(mnlistdiff.coinBaseTx);
            SimplifiedQuorumList newQuorumList = null;
            if(version >= 2) {
                newQuorumList = quorumList.applyDiff(mnlistdiff);
                newQuorumList.verify(mnlistdiff.coinBaseTx, newMNList);
                quorumList = newQuorumList;
            }
            mnList = newMNList;
            tipHeight = newHeight;
            tipBlockHash = mnlistdiff.blockHash;
            pendingRequests.remove(tipBlockHash);
            log.info(this.toString());
            unCache();
            if(mnlistdiff.hasChanges()) {
                if(mnlistdiff.coinBaseTx.getExtraPayloadObject().getVersion() >= 2 && quorumList.size() > 0)
                    setFormatVersion(LLMQ_FORMAT_VERSION);
                save();
            }
            //mnListSyncThread.notifyAll();
        } catch(IllegalArgumentException x) {
            //we already have this mnlistdiff or doesn't match our current tipBlockHash
            log.info(x.getMessage());
        } catch(NullPointerException x) {
            //file name is not set, do not save
            log.info(x.getMessage());
        } catch(BlockStoreException x) {
            log.info(x.getMessage());
            throw new ProtocolException(x);
        } finally {
            lock.unlock();
        }
    }

    public NewBestBlockListener newBestBlockListener = new NewBestBlockListener() {
        @Override
        public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
            if(isDeterministicMNsSporkActive()) {
               // if (Utils.currentTimeSeconds() - block.getHeader().getTimeSeconds() < 60 * 60)
                    requestMNListDiff(block);
            }
        }
    };

    public PeerConnectedEventListener peerConnectedEventListener = new PeerConnectedEventListener() {
        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            /*if(isDeterministicMNsSporkActive()) {
                if (tipBlockHash.equals(Sha256Hash.ZERO_HASH) || tipHeight < blockChain.getBestChainHeight()) {
                    if(Utils.currentTimeSeconds() - blockChain.getChainHead().getHeader().getTimeSeconds() < 60 * 60)
                        requestMNListDiff(peer, blockChain.getChainHead());
                }
            }*/
        }
    };

    public void setBlockChain(AbstractBlockChain blockChain, PeerGroup peerGroup) {
        this.blockChain = blockChain;
        blockChain.addNewBestBlockListener(newBestBlockListener);
        peerGroup.addConnectedEventListener(peerConnectedEventListener);
        peerGroup.addPreMessageReceivedEventListener(rejectionListener);
    }

    public void requestMNListDiff(StoredBlock block) {
        Peer peer = context.peerGroup.getDownloadPeer();
        if(peer == null) {
            List<Peer> peers = context.peerGroup.getConnectedPeers();
            peer = peers.get(new Random().nextInt(peers.size()));
        }
        if(peer != null)
            requestMNListDiff(peer, block);
    }

    public void requestMNListDiff(Peer peer, StoredBlock block) {
        Sha256Hash hash = block.getHeader().getHash();
        log.info("getmnlistdiff:  current block:  " + tipHeight + " requested block " + block.getHeight());

        lock.lock();
        try {
            //If we are requesting the block we have already, then skip the request
            if (hash.equals(tipBlockHash) && !hash.equals(Sha256Hash.ZERO_HASH))
                return;
            if(block.getHeight() < params.getDeterministicMasternodesEnabledHeight())
                return;

            //If we are requesting the block we have already, then skip the request
            if(hash.equals(tipBlockHash) && !hash.equals(Sha256Hash.ZERO_HASH))
                return;
/*
            if (lastRequestHash.equals(tipBlockHash)) {
                lastRequestCount++;
                if (lastRequestCount > 24) {
                    lastRequestCount = 0;
                    tipBlockHash = Sha256Hash.ZERO_HASH;
                    tipHeight = 0;
                    mnList = new SimplifiedMasternodeList(params);
                }
                log.info("Requesting the same mnlistdiff " + lastRequestCount + " times");
                if (lastRequestCount > 5) {
                    log.info("Stopping at 5 times to wait for a reply");
                    return;
                }
            } else {
                lastRequestCount = 0;
            }
            peer.sendMessage(new GetSimplifiedMasternodeListDiff(tipBlockHash, hash));
            lastRequestHash = tipBlockHash;
            */

            if (pendingRequests.size() == 0 && (tipHeight == -1 || tipBlockHash.equals(Sha256Hash.ZERO_HASH) || tipHeight < (block.getHeight() - 2000))) {
                requestingFirstList = true;
                pendingRequests.add(new StoredBlock(params.getGenesisBlock(), BigInteger.valueOf(0), 0));
            }
            pendingRequests.add(block);
            currentPeer = peer;
            processNextMNListDiff();
        } finally {
            lock.unlock();
        }
        //if(tipHeight == -1 || tipBlockHash.equals(Sha256Hash.ZERO_HASH))
        //    requestingFirstList = true;


        /*if(lastRequestHash.equals(tipBlockHash)) {
            lastRequestCount++;
            if(lastRequestCount > 24) {
                lastRequestCount = 0;
                tipBlockHash = Sha256Hash.ZERO_HASH;
                tipHeight = 0;
                mnList = new SimplifiedMasternodeList(params);
            }
            log.info("Requesting the same mnlistdiff " + lastRequestCount + " times");
            if(lastRequestCount > 5) {
                log.info("Stopping at 5 times to wait for a reply");
                return;
            }
        } else {
            lastRequestCount = 0;
        }*/
        //StoredBlock tipBlock = !tipBlockHash.equals(Sha256Hash.ZERO_HASH) ? blockChain.getBlockStore().get(tipBlockHash) : null;
        /*if(!requestingFirstList || (tipHeight == -1 || tipBlockHash.equals(Sha256Hash.ZERO_HASH) || tipHeight < (block.getHeight() - 2000))) {
            requestingFirstList = true;
            tipHeight = -1;
            tipBlockHash = Sha256Hash.ZERO_HASH;
            mnList = new SimplifiedMasternodeList(params);
            requestFullMNListDiff(peer, hash);
        }
        else {*/

        //    if(block.getHeader().getPrevBlockHash().equals(tipBlockHash))
         //       requestMNListDiff(peer, hash);
        //}
        //lastRequestHash = tipBlockHash;
    }

    void processNextMNListDiff() {
        if(!pendingRequests.isEmpty()) {
            StoredBlock block = pendingRequests.get(0);
            if(block.getHeight() == 0) {
                requestingFirstList = true;
                tipHeight = -1;
                tipBlockHash = Sha256Hash.ZERO_HASH;
                mnList = new SimplifiedMasternodeList(params);
                requestFullMNListDiff(context.peerGroup.getDownloadPeer(), block.getHeader().getHash());
            } else {
                requestMNListDiff(currentPeer, block.getHeader().getHash());
            }
        }
    }

    void requestMNListDiff(Peer peer, Sha256Hash hash) {
        peer.sendMessage(new GetSimplifiedMasternodeListDiff(tipBlockHash, hash));
    }

    void requestFullMNListDiff(Peer peer, Sha256Hash toBlockHash) {
        peer.sendMessage(new GetSimplifiedMasternodeListDiff(Sha256Hash.ZERO_HASH, toBlockHash));
    }

    public void updateMNList() {
        requestMNListDiff(context.blockChain.getChainHead());
    }

    @Override
    public String toString() {
        return "SimplifiedMNListManager:  {" + mnList + ", tipHeight "+ tipHeight +"}";
    }

    public long getSpork15Value() {
        return context.sporkManager.getSporkValue(SporkManager.SPORK_15_DETERMINISTIC_MNS_ENABLED);
    }

    public boolean isDeterministicMNsSporkActive(long height) {
        if(height == -1) {
            height = tipHeight;
        }

        return height > params.getDeterministicMasternodesEnabledHeight();
    }

    public boolean isDeterministicMNsSporkActive() {
        return isDeterministicMNsSporkActive(-1) || params.isDeterministicMasternodesEnabled();
    }

    public SimplifiedMasternodeList getListAtChainTip() {
        return mnList;
    }

    public SimplifiedQuorumList getQuorumListAtTip() {
        return quorumList;
    }

    @Override
    public int getCurrentFormatVersion() {
        return quorumList.size() != 0 ? LLMQ_FORMAT_VERSION : DMN_FORMAT_VERSION;
    }

    public void resetMNList() {
        if(getFormatVersion() < LLMQ_FORMAT_VERSION) {
            tipHeight = -1;
            tipBlockHash = Sha256Hash.ZERO_HASH;
            mnList = new SimplifiedMasternodeList(context.getParams());
            requestMNListDiff(blockChain.getChainHead());
        }
    }

    Thread mnListSyncThread = new Thread("MasternodeListSync") {
        @Override
        public void run() {
            //lock.lock();
            try {
                while(true) {
                    lock.lock();
                    try {
                        if (!pendingRequests.isEmpty()) {
                            Iterator<StoredBlock> itblock = pendingRequests.iterator();
                            while (itblock.hasNext()) {
                                StoredBlock block = itblock.next();
                                if ((tipHeight == -1 || tipBlockHash.equals(Sha256Hash.ZERO_HASH) || tipHeight < (block.getHeight() - 2000))) {
                                    requestingFirstList = true;
                                    tipHeight = -1;
                                    tipBlockHash = Sha256Hash.ZERO_HASH;
                                    mnList = new SimplifiedMasternodeList(params);
                                    requestFullMNListDiff(context.peerGroup.getDownloadPeer(), block.getHeader().getHash());
                                    lock.unlock();
                                    wait();
                                    lock.lock();
                                    requestingFirstList = false;
                                } else {
                                    requestMNListDiff(context.peerGroup.getDownloadPeer(), block.getHeader().getHash());
                                    lock.unlock();
                                    wait();
                                    lock.lock();
                                }
                                lock.lock();
                                itblock.remove();
                                lock.unlock();
                            }
                        }
                        sleep(5000);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch(InterruptedException x) {

            } finally {
                //lock.unlock();
            }
        }
    };

    public void start() {
        if(!mnListSyncThread.isAlive())
            mnListSyncThread.start();
    }


    private PreMessageReceivedEventListener rejectionListener = new PreMessageReceivedEventListener() {
        @Override
        public Message onPreMessageReceived(Peer peer, Message m) {
            if (m instanceof RejectMessage) {
                RejectMessage rejectMessage = (RejectMessage)m;
                log.info(rejectMessage.getReasonString());
            }
            return m;
        }
    };
}
