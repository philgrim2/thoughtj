package live.thought.thoughtj.governance;

import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.core.ProtocolException;

/**
 * Created by Eric on 6/21/2018.
 */
public class GovernanceObjectFromFile extends GovernanceObject {

    public GovernanceObjectFromFile(NetworkParameters params, byte[] payload, int cursor) {
        super(params, payload, cursor);
    }

    @Override
    protected void parse() throws ProtocolException {
        super.parse();
        parseFromDisk();
        length = cursor - offset;
    }
}