package de.bjusystems.vdrmanager.ng.tasks;

import android.app.Activity;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Timer;
import de.bjusystems.vdrmanager.ng.gui.CertificateProblemDialog;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient.TimerOperation;

public abstract class DeleteTimerTask extends AsyncProgressTask<Timer> {

  public DeleteTimerTask(final Activity activity, final Timer timer) {
    super(activity, new SetTimerClient(timer, TimerOperation.DELETE, new CertificateProblemDialog(activity)) {
      @Override
      public int getProgressTextId() {
        return R.string.progress_timer_delete;
      }
    });
  }
}
