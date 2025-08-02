package de.bjusystems.vdrmanager.ng.tasks;

import android.app.Activity;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Timer;
import de.bjusystems.vdrmanager.ng.gui.CertificateProblemDialog;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SetTimerClient.TimerOperation;

public abstract class ModifyTimerTask extends AsyncProgressTask<Timer> {

  public ModifyTimerTask(final Activity activity, final Timer newTimer, final Timer oldTimer) {
    super(activity, new SetTimerClient(newTimer, oldTimer, TimerOperation.MODIFY, new CertificateProblemDialog(activity)) {
      @Override
      public int getProgressTextId() {
        return R.string.progress_timer_modify;
      }


    });


  }
}
