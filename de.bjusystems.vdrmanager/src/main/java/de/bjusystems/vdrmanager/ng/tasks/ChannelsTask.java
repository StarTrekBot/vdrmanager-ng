package de.bjusystems.vdrmanager.ng.tasks;

import android.app.Activity;
import de.bjusystems.vdrmanager.ng.data.Channel;
import de.bjusystems.vdrmanager.ng.utils.svdrp.ChannelClient;

public abstract class ChannelsTask  extends AsyncProgressTask<Channel> {
  public ChannelsTask(final Activity activity, final ChannelClient client) {
    super(activity, client);
  }
}
