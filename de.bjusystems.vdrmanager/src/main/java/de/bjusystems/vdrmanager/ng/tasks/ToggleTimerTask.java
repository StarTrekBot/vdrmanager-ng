package de.bjusystems.vdrmanager.ng.tasks;

import android.app.Activity;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Timer;
import de.bjusystems.vdrmanager.ng.gui.CertificateProblemDialog;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient.TimerOperation;

public abstract class ToggleTimerTask extends AsyncProgressTask<Timer> {

  public ToggleTimerTask(final Activity activity, final Timer timer) {
    super(activity, new SetTimerClient(timer, TimerOperation.TOGGLE, new CertificateProblemDialog(activity)) {
      boolean enabled = timer.isEnabled();

      @Override
      public int getProgressTextId() {
        if (enabled) {
          return R.string.progress_timer_disable;
        } else {
          return R.string.progress_timer_enable;
        }
      }
    });
    timer.setEnabled(!timer.isEnabled());
  }
}
