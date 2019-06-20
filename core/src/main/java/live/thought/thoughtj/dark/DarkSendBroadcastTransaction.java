package live.thought.thoughtj.dark;

import live.thought.thoughtj.core.Transaction;
import live.thought.thoughtj.core.TransactionInput;

/**
 * Created by Eric on 2/8/2015.
 */
@Deprecated
public class DarkSendBroadcastTransaction {
    Transaction tx;
    TransactionInput vin;
    byte [] vchSig;
    long sigTime;
}
