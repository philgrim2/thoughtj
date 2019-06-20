package live.thought.thoughtj.core.listeners;

import live.thought.thoughtj.core.SporkMessage;

public interface SporkUpdatedEventListener {
    void onSporkUpdated(SporkMessage sporkMessage);
}