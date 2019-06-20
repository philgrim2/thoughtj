package live.thought.thoughtj.kits;

import live.thought.thoughtj.core.Context;
import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.store.BlockStore;
import live.thought.thoughtj.store.BlockStoreException;
import live.thought.thoughtj.store.LevelDBBlockStore;
import live.thought.thoughtj.store.SPVBlockStore;

import java.io.File;

/**
 * Created by Eric on 2/23/2016.
 */
public class LevelDBWalletAppKit extends WalletAppKit {
    public LevelDBWalletAppKit(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    /**
     * Override this to use a {@link BlockStore} that isn't the default of {@link SPVBlockStore}.
     */
    protected BlockStore provideBlockStore(File file) throws BlockStoreException {
        return new LevelDBBlockStore(context, file);
    }
}
