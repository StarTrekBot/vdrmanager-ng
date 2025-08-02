package de.bjusystems.vdrmanager.ng.tasks;

import android.app.Activity;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Recording;
import de.bjusystems.vdrmanager.ng.gui.CertificateProblemDialog;
import de.bjusystems.vdrmanager.ng.utils.svdrp.DelRecordingClient;

public abstract class DeleteRecordingTask extends AsyncProgressTask<Recording> {
  public DeleteRecordingTask(final Activity activity, final Recording r) {
    super(activity, new DelRecordingClient(r, new CertificateProblemDialog(activity)) {
      @Override
      public int getProgressTextId() {
        return R.string.progress_recording_delete;
      }
    });
  }
}
