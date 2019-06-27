package live.thought.thoughtj.core;

import java.math.BigInteger;
import java.util.Map;


public class CoinDefinition {


    public static final String coinName = "Thought";
    public static final String coinTicker = "THT";
    public static final String coinURIScheme = "thought";
    //public static final String cryptsyMarketId = "155";
    //public static final String cryptsyMarketCurrency = "BTC";
    public static final String PATTERN_PRIVATE_KEY_START_UNCOMPRESSED = "[K]";
    public static final String PATTERN_PRIVATE_KEY_START_COMPRESSED = "[X]"; //ajh need to check if we need this

    public enum CoinPrecision {
        Coins,
        Millicoins,
    }
    public static final CoinPrecision coinPrecision = CoinPrecision.Coins;

    public static final String UNSPENT_API_URL = "https://chainz.cryptoid.info/dash/api.dws?q=unspent";
    public enum UnspentAPIType {
        BitEasy,
        Blockr,
        Abe,
        Cryptoid,
    };
    //public static final UnspentAPIType UnspentAPI = UnspentAPIType.Cryptoid;

    //public static final String BLOCKEXPLORER_BASE_URL_PROD = "http://explorer.dash.org/";    //blockr.io
    //public static final String BLOCKEXPLORER_ADDRESS_PATH = "address/";             //blockr.io path
    //public static final String BLOCKEXPLORER_TRANSACTION_PATH = "tx/";              //blockr.io path
    //public static final String BLOCKEXPLORER_BLOCK_PATH = "block/";                 //blockr.io path
    //public static final String BLOCKEXPLORER_BASE_URL_TEST = "http://test.explorer.dash.org/";

    //public static final String DONATION_ADDRESS = "Xdeh9YTLNtci5zSL4DDayRSVTLf299n9jv";  //Hash Engineering donation DASH address

    enum CoinHash {
        SHA256,
        scrypt,
        x11
    };
    public static final CoinHash coinPOWHash = CoinHash.SHA256;

    public static boolean checkpointFileSupport = true;

    public static final int TARGET_TIMESPAN = (int)(1.618 * 24 * 60 * 60); // Thought: 1 day
    public static final int TARGET_SPACING = (int)(1.618 * 60); // Thought: 1.6 minutes
    public static final int INTERVAL = TARGET_TIMESPAN / TARGET_SPACING;

    public static final int getInterval(int height, boolean testNet) {
            return INTERVAL;
    }
    public static final int getIntervalCheckpoints() {
            return INTERVAL;

    }
    public static final int getTargetTimespan(int height, boolean testNet) {
            return TARGET_TIMESPAN;
    }

    public static int spendableCoinbaseDepth = 100; //main.h: static const int COINBASE_MATURITY
    public static final long MAX_COINS = 1618000000;                 //chainparams.cpp


    public static final long DEFAULT_MIN_TX_FEE = 10000;   // MIN_TX_FEE
    public static final long DUST_LIMIT = 5460; //Transaction.h CTransaction::GetDustThreshold for 10000 MIN_TX_FEE
    public static final long INSTANTX_FEE = 100000; //0.001 DASH (updated for 12.1)
    public static final boolean feeCanBeRaised = false;

    //
    // Dash 0.12.1.x
    //
    public static final int PROTOCOL_VERSION = 70017;          //version.h PROTOCOL_VERSION
    public static final int MIN_PROTOCOL_VERSION = 70016;        //version.h MIN_PROTO_VERSION

    public static final int BLOCK_CURRENTVERSION = 7;   //CBlock::CURRENT_VERSION
    public static final int MAX_BLOCK_SIZE = 4 * 1000 * 1000;


    public static final boolean supportsBloomFiltering = true; //Requires PROTOCOL_VERSION 70000 in the client

    public static final int Port    = 10618;       //protocol.h GetDefaultPort(testnet=false)
    public static final int TestPort = 11618;     //protocol.h GetDefaultPort(testnet=true)

    //
    //  Production
    //
    public static final int AddressHeader = 7;             //base58.h CBitcoinAddress::PUBKEY_ADDRESS
    public static final int p2shHeader = 9;             //base58.h CBitcoinAddress::SCRIPT_ADDRESS
    public static final int dumpedPrivateKeyHeader = 123;   //common to all coins
    public static final long oldPacketMagic = 0xfbc0b6db;      //0xfb, 0xc0, 0xb6, 0xdb - ajh - this is bitcoins
    public static final long PacketMagic = 0x59472ee4; //ajh - this is thought's

    //Genesis Block Information from main.cpp: LoadBlockIndex
    static public long genesisBlockDifficultyTarget = (0x1d00ffffL);
    static public long genesisBlockTime = 1521039602L;
    static public long genesisBlockNonce = (2074325340);
    static public String genesisHash = "00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"; //chainparams.cpp: hashGenesisBlock
    static public String genesisMerkleRoot = "483a98bfa350f319e52eceaa79585fab8e5ac49c6235f720915e9c671a03c2d6";
    static public int genesisBlockValue = 1618;       // Not sure where this is


    // TxOut from chainparams.cpp.  Not sure yet what to use for TxIn.  Block explorer doesn't show one.
    static public String genesisTxInBytes = "04ffff001d01044c5b55534120546f6461792031342f4d61722f32303138204861776b696e6727732064656174682c2045696e737465696e27732062697274682c20616e64205069204461793a207768617420646f657320697420616c6c206d65616e3f";   //"limecoin se convertira en una de las monedas mas segura del mercado, checa nuestros avances"
    static public String genesisTxOutBytes = "04ed28f11f74795344edfdbc1fccb1e6de37c909ab0c2a535aa6a054fca6fd34b05e3ed9822fa00df98698555d7582777afbc355ece13b7a47004ffe58c0b66c08";

    //net.cpp strDNSSeed
    static public String[] dnsSeeds = new String[] {
            "phee.thought.live",
            "phi.thought.live",
            "pho.thought.live",
            "phum.thought.live",
    };


    public static int minBroadcastConnections = 0;   //0 for default; Using 3 like BreadWallet.

    //
    // TestNet - DASH
    //
    public static final boolean supportsTestNet = true;
    public static final int testnetAddressHeader = 109;             //base58.h CBitcoinAddress::PUBKEY_ADDRESS_TEST
    public static final int testnetp2shHeader = 193;             //base58.h CBitcoinAddress::SCRIPT_ADDRESS_TEST
    public static final long testnetPacketMagic = 0x2b9939bf;      //
    public static final String testnetGenesisHash = "00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b";
    static public long testnetGenesisBlockDifficultyTarget = (0x1d00ffffL);         //main.cpp: LoadBlockIndex
    static public long testnetGenesisBlockTime = 1521039602L;                       //main.cpp: LoadBlockIndex
    static public long testnetGenesisBlockNonce = (2074325340L);                         //main.cpp: LoadBlockIndex





    //main.cpp GetBlockValue(height, fee)
    public static final Coin GetBlockReward(int height)
    {
        Coin nSubsidy = (height == 1 ? Coin.valueOf(809016994, 0) : Coin.valueOf(314, 0));
        return nSubsidy;
    }

    public static int subsidyDecreaseBlockCount = 1299382;     //main.cpp GetBlockValue(height, fee) //ajh halving interval

    public static BigInteger proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);  //main.cpp bnProofOfWorkLimit (~uint256(0) >> 20); // digitalcoin: starting difficulty is 1 / 2^12

    static public String[] testnetDnsSeeds = new String[] {
            "phi.thought.live",
            "phee.thought.live",
    };
    //from main.h: CAlert::CheckSignature
    public static final String SATOSHI_KEY = "048240a8748a80a286b270ba126705ced4f2ce5a7847b3610ea3c06513150dade2a8512ed5ea86320824683fc0818f0ac019214973e677acd1244f6d0571fc5103";
    public static final String TESTNET_SATOSHI_KEY = "04517d8a699cb43d3938d7b24faaff7cda448ca4ea267723ba614784de661949bf632d6304316b244646dea079735b9a6fc4af804efb4752075b9fe2245e14e412";

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_MAINNET = "live.thought.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_TESTNET = "live.thought.test";
    /** Unit test network. */
    public static final String ID_UNITTESTNET = "live.thought.unittest";

    //checkpoints.cpp Checkpoints::mapCheckpoints
    public static void initCheckpoints(Map<Integer, Sha256Hash> checkpoints)
    {

        checkpoints.put(  0, Sha256Hash.wrap("00000000917e049641189c33d6b1275155e89b7b498b3b4f16d488f60afe513b"));
        checkpoints.put(  2, Sha256Hash.wrap("00000000c4c1989f0979bae2b24840b48ddb5197866a8ee99c9399a2512ec588"));
        checkpoints.put(  5, Sha256Hash.wrap("000000003a062431a6e4430a6ade4ab402a29165462491338c98b336a8afb6ab"));
        checkpoints.put( 256, Sha256Hash.wrap("00000000acf5b9f9eb1ea8c56f07ff00c2e3b5335c1b574f98cc3b8b55b70ec3"));
        checkpoints.put( 1024, Sha256Hash.wrap("000000006aef3c0953d44120c972061811aca7a59167076573f9063e46265419"));
        checkpoints.put( 43010, Sha256Hash.wrap("00000000328f2e44914cf6af972de811d0f4869f9b4e9217e4093dd297c79f49"));
        checkpoints.put( 229731, Sha256Hash.wrap("000000006645878b6aa7c4f10044b9914e994f11e1c3905c72b7f7612c417a94"));
        checkpoints.put( 248000, Sha256Hash.wrap("006b52a5d017eb2590d25750c46542b2de43f7a3fdc6394d95db458cbcb35f85"));
        checkpoints.put( 388285, Sha256Hash.wrap("00e0d38562e2f576c3c501f4768b282824a7f9489778537c49e3b5492923f5c5"));
    }

    //Unit Test Information
    public static final String UNITTEST_ADDRESS = "XgxQxd6B8iYgEEryemnJrpvoWZ3149MCkK";
    public static final String UNITTEST_ADDRESS_PRIVATE_KEY = "XDtvHyDHk4S3WJvwjxSANCpZiLLkKzoDnjrcRhca2iLQRtGEz1JZ";

}
