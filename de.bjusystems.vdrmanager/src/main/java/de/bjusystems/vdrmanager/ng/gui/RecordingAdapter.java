package de.bjusystems.vdrmanager.ng.gui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.bjusystems.vdrmanager.ng.R;
import de.bjusystems.vdrmanager.ng.data.Event;
import de.bjusystems.vdrmanager.ng.data.EventFormatter;
import de.bjusystems.vdrmanager.ng.data.EventListItem;
import de.bjusystems.vdrmanager.ng.data.Recording;
import de.bjusystems.vdrmanager.ng.data.RecordingListItem;
import de.bjusystems.vdrmanager.ng.databinding.FolderItemBinding;

class RecordingAdapter extends BaseEventAdapter<EventListItem> {

	protected final static int TYPE_FOLDER = 2;

	public RecordingAdapter(final Context context) {
		super(context, R.layout.epg_event_item);
		hideChannelName = false;
	}

	@Override
	protected EventFormatter getEventFormatter(Event event) {
		return new EventFormatter(event, true);
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	protected boolean isHeader(EventListItem item) {
		if (item instanceof RecordingListItem == false) {
			return item.isHeader();
		}

		if (((RecordingListItem) item).isFolder()) {
			return false;
		}

		return item.isHeader();
	}

	@Override
	public int getItemViewType(int position) {

		// get item
		final RecordingListItem item = (RecordingListItem) getItem(position);

		if (item.isHeader()) {
			return TYPE_HEADER;
		} else if (item.isFolder()) {
			return TYPE_FOLDER;
		}
		return TYPE_ITEM;
	}

	class EventListItemFolderHolder {
		public FolderItemBinding binding;
	}

	@Override
	public View getView(final int position, View convertView,
			final ViewGroup parent) {

		// get item
		final RecordingListItem item = (RecordingListItem) getItem(position);

		if (item.isFolder() == false) {
			return super.getView(position, convertView, parent);
		}

		EventListItemFolderHolder holder = null;
		if (convertView == null
				|| (convertView != null && convertView.getTag() instanceof EventListItemFolderHolder) == false) {
			FolderItemBinding binding = FolderItemBinding.inflate(inflater, parent, false);
			convertView = binding.getRoot();
			holder = new EventListItemFolderHolder();
			holder.binding = binding;
			convertView.setTag(holder);
		} else {
			holder = (EventListItemFolderHolder) convertView.getTag();
		}

		holder.binding.headerItem
				.setText(Utils.highlight(item.folder.getName(), highlight));
		holder.binding.count.setText(String.valueOf(item.folder.size()));
		return convertView;
	}

	@Override
	public RecordingListItem getItem(int position) {
		return (RecordingListItem) super.getItem(position);
	}

	//
	// protected void addSuper(RecordingListItem item) {
	// super.addSuper(item);
	// }
	//
	// protected void clearSuper() {
	// super.clear();
	// }
	protected void handleState(EventListItemHolder itemHolder,
			EventListItem item) {

		Recording r = (Recording) item.getEvent();
		if (r.getTimerStopTime() != null) {
			itemHolder.binding.timerItemState.setImageResource(R.drawable.timer_recording);
		} else {
			itemHolder.binding.timerItemState.setImageResource(R.drawable.timer_none);
			itemHolder.binding.timerItemOther.setVisibility(View.GONE);
			if (r.isNeww() == true) {
				itemHolder.binding.timerItemState.setImageResource(R.drawable.newrecording);
				if (r.isCut()) {
					itemHolder.binding.timerItemOther.setVisibility(View.VISIBLE);
					itemHolder.binding.timerItemOther.setImageResource(R.drawable.schere);
				} else {
					itemHolder.binding.timerItemOther.setVisibility(View.GONE);
				}
			} else if (r.isCut()) {
				itemHolder.binding.timerItemState.setImageResource(R.drawable.schere);
			}
		}

	}

}