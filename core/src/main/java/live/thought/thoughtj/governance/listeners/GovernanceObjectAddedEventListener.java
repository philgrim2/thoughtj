package live.thought.thoughtj.governance.listeners;

import live.thought.thoughtj.core.Sha256Hash;
import live.thought.thoughtj.governance.GovernanceObject;

public interface GovernanceObjectAddedEventListener {
    void onGovernanceObjectAdded(Sha256Hash nHash, GovernanceObject object);
}