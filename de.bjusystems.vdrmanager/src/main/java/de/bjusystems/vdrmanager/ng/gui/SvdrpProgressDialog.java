package de.bjusystems.vdrmanager.ng.gui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpClient;

import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpEvent;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpException;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpExceptionListener;
import de.bjusystems.vdrmanager.ng.utils.svdrp.SvdrpListener;

public class SvdrpProgressDialog<T> extends ProgressDialog implements
		SvdrpExceptionListener, SvdrpListener, DialogInterface.OnCancelListener {

	ProgressDialog progress;

	SvdrpClient<? extends Object> client;

	public SvdrpProgressDialog(final Context context,
			final SvdrpClient<T> client) {
		super(context);

		this.client = client;
		progress = new ProgressDialog(context);
		progress.setOnCancelListener(this);
		progress.setCancelable(true);
		progress.setCanceledOnTouchOutside(false);
	}

	public void svdrpEvent(final SvdrpEvent sevent) {
		if (progress == null) {
			return;
		}
		switch (sevent) {
			case CONNECTING:
				progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progress.setMessage(getContext().getString(R.string.progress_connect));
				progress.show();
				break;
			case COMMAND_SENT:
				progress.setMessage(getContext().getString(client.getProgressTextId()));
				break;
			case ABORTED:
			case CONNECTION_TIMEOUT:
			case CONNECT_ERROR:
			case ERROR:
			case LOGIN_ERROR:
			case FINISHED_ABNORMALY:
			case FINISHED_SUCCESS:
			case CACHE_HIT:
				progress.dismiss();
				break;
			case DISCONNECTED:
				break;
		}
	}



	public void svdrpException(final SvdrpException exception) {

	}

	private void abort() {
		if (client != null) {
			client.abort();
		}
		dismiss();
	}


	public void dismiss() {
		if (progress != null && progress.isShowing()) {
			progress.dismiss();
		}
	}


	public void onCancel(DialogInterface dialog) {
		abort();
	}

	@Override
	public void svdrpEvent(SvdrpEvent event, Throwable t) {
		this.svdrpEvent(event);
		if (t != null) {
			String msg = t.getLocalizedMessage();
			if (msg == null) {
				msg = t.getMessage();
			}
			if (msg != null) {
				Utils.say(getContext(), msg);
			}
		}
	}

}
