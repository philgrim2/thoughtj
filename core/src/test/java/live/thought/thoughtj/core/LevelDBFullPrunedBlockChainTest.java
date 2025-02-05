/*
 * Copyright 2016 Robin Owens
 *
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

import org.junit.After;

import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.store.BlockStoreException;
import live.thought.thoughtj.store.FullPrunedBlockStore;
import live.thought.thoughtj.store.LevelDBFullPrunedBlockStore;

import java.io.File;

/**
 * An H2 implementation of the FullPrunedBlockStoreTest
 */
public class LevelDBFullPrunedBlockChainTest extends
        AbstractFullPrunedBlockChainTest {
    @After
    public void tearDown() throws Exception {
        deleteFiles();
    }

    @Override
    public FullPrunedBlockStore createStore(NetworkParameters params,
            int blockCount) throws BlockStoreException {
        deleteFiles();
        return new LevelDBFullPrunedBlockStore(params, "test-leveldb",
                blockCount);
    }

    private void deleteFiles() {
        File f = new File("test-leveldb");
        if (f != null && f.exists()) {
            for (File c : f.listFiles())
                c.delete();
        }
    }

    @Override
    public void resetStore(FullPrunedBlockStore store)
            throws BlockStoreException {
        ((LevelDBFullPrunedBlockStore) store).resetStore();
    }
}
