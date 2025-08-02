package de.bjusystems.vdrmanager.ng.gui;

import android.content.Context;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Event;
import de.bjusystems.vdrmanager.ng.data.EventFormatter;
import de.bjusystems.vdrmanager.ng.data.EventListItem;

class ChannelEventAdapter extends BaseEventAdapter<EventListItem>
{


	public ChannelEventAdapter(final Context context) {
		super(context, R.layout.epg_event_item);
		hideChannelName = true;
	}

	@Override
	protected EventFormatter getEventFormatter(Event event) {
			return new EventFormatter(event,true);
	}
}