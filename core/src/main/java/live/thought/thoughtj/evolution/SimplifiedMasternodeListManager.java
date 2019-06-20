package live.thought.thoughtj.evolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import live.thought.thoughtj.core.*;
import live.thought.thoughtj.core.listeners.NewBestBlockListener;
import live.thought.thoughtj.core.listeners.PeerConnectedEventListener;
import live.thought.thoughtj.utils.Threading;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class SimplifiedMasternodeListManager extends AbstractManager {
    private static final Logger log = LoggerFactory.getLogger(MasternodeManager.class);
    private ReentrantLock lock = Threading.lock("SimplifiedMasternodeListManager");


    public static final int SNAPSHOT_LIST_PERIOD = 576; // once per day
    public static final int LISTS_CACHE_SIZE = 576;

    HashMap<Sha256Hash, SimplifiedMasternodeList> mnListsCache;
    SimplifiedMasternodeList mnList;
    long tipHeight;
    Sha256Hash tipBlockHash;

    AbstractBlockChain blockChain;

    Sha256Hash lastRequestHash = Sha256Hash.ZERO_HASH;
    int lastRequestCount;

    public SimplifiedMasternodeListManager(Context context) {
        super(context);
        tipBlockHash = Sha256Hash.ZERO_HASH;
        mnList = new SimplifiedMasternodeList(context.getParams());
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
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        lock.lock();
        try {
            mnList.bitcoinSerialize(stream);
            stream.write(tipBlockHash.getReversedBytes());
            Utils.uint32ToByteStreamLE(tipHeight, stream);
        } finally {
            lock.unlock();
        }
    }

    public void updatedBlockTip(StoredBlock tip) {
    }

    public void processMasternodeListDiff(SimplifiedMasternodeListDiff mnlistdiff) {
        long newHeight = ((CoinbaseTx) mnlistdiff.coinBaseTx.getExtraPayloadObject()).getHeight();
        log.info("processing mnlistdiff between : " + tipHeight + " & " + newHeight + "; " + mnlistdiff);
        lock.lock();
        try {
            SimplifiedMasternodeList newMNList = mnList.applyDiff(mnlistdiff);
            newMNList.verify(mnlistdiff.coinBaseTx);
            mnList = newMNList;
            tipHeight = newHeight;
            tipBlockHash = mnlistdiff.blockHash;
            log.info(this.toString());
            unCache();
            if(mnlistdiff.hasChanges())
                save();
        } catch(IllegalArgumentException x) {
            //we already have this mnlistdiff or doesn't match our current tipBlockHash
            log.info(x.getMessage());
        } catch(NullPointerException x) {
            //file name is not set, do not save
            log.info(x.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public NewBestBlockListener newBestBlockListener = new NewBestBlockListener() {
        @Override
        public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
            if(isDeterministicMNsSporkActive()) {
                if (Utils.currentTimeSeconds() - block.getHeader().getTimeSeconds() < 60 * 60)
                    requestMNListDiff(block);
            }
        }
    };

    public PeerConnectedEventListener peerConnectedEventListener = new PeerConnectedEventListener() {
        @Override
        public void onPeerConnected(Peer peer, int peerCount) {
            if(isDeterministicMNsSporkActive()) {
                if (tipBlockHash.equals(Sha256Hash.ZERO_HASH) || tipHeight < blockChain.getBestChainHeight()) {
                    if(Utils.currentTimeSeconds() - blockChain.getChainHead().getHeader().getTimeSeconds() < 60 * 60)
                        requestMNListDiff(peer, blockChain.getChainHead());
                }
            }
        }
    };

    public void setBlockChain(AbstractBlockChain blockChain, PeerGroup peerGroup) {
        this.blockChain = blockChain;
        blockChain.addNewBestBlockListener(newBestBlockListener);
        peerGroup.addConnectedEventListener(peerConnectedEventListener);
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
        } finally {
            lock.unlock();
        }
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
}
