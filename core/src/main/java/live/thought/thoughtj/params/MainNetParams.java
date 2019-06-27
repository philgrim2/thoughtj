/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

package live.thought.thoughtj.params;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import live.thought.thoughtj.core.*;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.*;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends AbstractThoughtNetParams {
    private static final Logger log = LoggerFactory.getLogger(MainNetParams.class);

    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public static final int MAINNET_MAJORITY_DIP0001_WINDOW = 100;
    public static final int MAINNET_MAJORITY_DIP0001_THRESHOLD = 2;

    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = CoinDefinition.proofOfWorkLimit;
        dumpedPrivateKeyHeader = 123;
        addressHeader = CoinDefinition.AddressHeader;
        p2shHeader = CoinDefinition.p2shHeader;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader};
        port = CoinDefinition.Port;
        packetMagic = CoinDefinition.PacketMagic;
        bip32HeaderPub = 0xFbC6A00D; //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderPriv = 0x5AEBD8C6; //The 4 byte header that serializes in base58 to "xprv"
        genesisBlock.setDifficultyTarget(CoinDefinition.genesisBlockDifficultyTarget);
        genesisBlock.setTime(CoinDefinition.genesisBlockTime);
        genesisBlock.setNonce(CoinDefinition.genesisBlockNonce);

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        id = ID_MAINNET;
        subsidyDecreaseBlockCount = CoinDefinition.subsidyDecreaseBlockCount;
        spendableCoinbaseDepth = CoinDefinition.spendableCoinbaseDepth;
        String genesisHash = genesisBlock.getHashAsString();


	System.out.println(genesisBlock.toString());
	byte[] gb = genesisBlock.bitcoinSerialize();
        System.out.println(bytesToHex(gb));			

        checkState(genesisHash.equals(CoinDefinition.genesisHash),
                genesisHash);

        //CoinDefinition.initCheckpoints(checkpoints);

        dnsSeeds = new String[] {
                "phee.thought.live",
                "phi.thought.live",
                "pho.thought.live",
                "phum.thought.live"
        };

        httpSeeds = null; /*new HttpDiscovery.Details[] {*/

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.

            checkpoints.put(  0, Sha256Hash.wrap("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"));
            checkpoints.put(  2, Sha256Hash.wrap("00000000c4c1989f0979bae2b24840b48ddb5197866a8ee99c9399a2512ec588"));
            checkpoints.put(  5, Sha256Hash.wrap("000000003a062431a6e4430a6ade4ab402a29165462491338c98b336a8afb6ab"));
            checkpoints.put( 256, Sha256Hash.wrap("00000000acf5b9f9eb1ea8c56f07ff00c2e3b5335c1b574f98cc3b8b55b70ec3"));
            checkpoints.put( 1024, Sha256Hash.wrap("000000006aef3c0953d44120c972061811aca7a59167076573f9063e46265419"));
            checkpoints.put( 43010, Sha256Hash.wrap("00000000328f2e44914cf6af972de811d0f4869f9b4e9217e4093dd297c79f49"));
            checkpoints.put( 229731, Sha256Hash.wrap("000000006645878b6aa7c4f10044b9914e994f11e1c3905c72b7f7612c417a94"));
            checkpoints.put( 248000, Sha256Hash.wrap("006b52a5d017eb2590d25750c46542b2de43f7a3fdc6394d95db458cbcb35f85"));
            checkpoints.put( 388285, Sha256Hash.wrap("00e0d38562e2f576c3c501f4768b282824a7f9489778537c49e3b5492923f5c5"));


        addrSeeds = new int[] {

        };

        strSporkAddress = "3vjBVUDb38RDsByGVFZ3AVkzB4eU1XJ9ox";
        budgetPaymentsStartBlock = 385627;
        budgetPaymentsCycleBlocks = 26700;
        budgetPaymentsWindowBlocks = 100;

        DIP0001Window = MAINNET_MAJORITY_DIP0001_WINDOW;
        DIP0001Upgrade = MAINNET_MAJORITY_DIP0001_THRESHOLD;
        DIP0001BlockHeight = 393500;

        fulfilledRequestExpireTime = 60*60;
        masternodeMinimumConfirmations = 15;
        superblockStartBlock = 614820;
        superblockCycle = 16616;
        nGovernanceMinQuorum = 40;
        nGovernanceFilterElements = 20000;

        powAllowMinimumDifficulty = false;
        powNoRetargeting = false;

        instantSendConfirmationsRequired = 6;
        instantSendKeepLock = 24;

        DIP0003BlockHeight = 1028160;
        deterministicMasternodesEnabledHeight = 1047200;
        deterministicMasternodesEnabled = true;
        
        maxCuckooTarget = new BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
        cuckooHardForkBlockHeight = 246500;
        cuckooRequiredBlockHeight = 248800;
        midasStartHeight = 337;
        midasValidHeight = 512;
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
    
    
}
