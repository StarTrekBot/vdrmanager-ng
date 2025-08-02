package de.bjusystems.vdrmanager.ng.gui;

import android.content.Context;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Event;
import de.bjusystems.vdrmanager.ng.data.EventFormatter;
import de.bjusystems.vdrmanager.ng.data.EventListItem;

public class TimeEventAdapter extends BaseEventAdapter<EventListItem> {

	public TimeEventAdapter(final Context context) {
		super(context, R.layout.epg_event_item);
	}

	@Override
	protected EventFormatter getEventFormatter(Event event) {
		return new EventFormatter(event, true);
	}
}