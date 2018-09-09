package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransactionGroupListAdapter
		extends GroupEditableListAdapter<TransactionGroupListAdapter.PreloadedGroup, GroupEditableListAdapter.GroupViewHolder>
{
	private AccessDatabase mDatabase;
	private SQLQuery.Select mSelect;
	private NumberFormat mPercentFormat;
	private ArrayList<Long> mRunningTasks = new ArrayList<>();

	public TransactionGroupListAdapter(Context context, AccessDatabase database)
	{
		super(context, MODE_GROUP_BY_DATE);

		mDatabase = database;
		mPercentFormat = NumberFormat.getPercentInstance();

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
	}

	@Override
	protected void onLoad(GroupLister<PreloadedGroup> lister)
	{
		ArrayList<Long> activeList = new ArrayList<>(mRunningTasks);

		for (PreloadedGroup group : mDatabase.castQuery(getSelect(), PreloadedGroup.class, new SQLiteDatabase.CastQueryListener<PreloadedGroup>()
		{
			@Override
			public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, PreloadedGroup object)
			{
				StringBuilder assigneesText = new StringBuilder();

				for (TransferAssigneeListAdapter.ShowingAssignee showingAssignee
						: TransferAssigneeListAdapter.loadAssigneeList(db, object.groupId)) {
					if (assigneesText.length() > 0)
						assigneesText.append(", ");

					assigneesText.append(showingAssignee.device.nickname);
				}

				object.assignees = assigneesText.length() > 0
						? assigneesText.toString()
						: getContext().getString(R.string.text_emptySymbol);
			}
		})) {
			mDatabase.calculateTransactionSize(group.groupId, group.index);

			group.isRunning = activeList.contains(group.groupId);
			group.totalCount = group.index.incomingCount + group.index.outgoingCount;
			group.totalBytes = group.index.incoming + group.index.outgoing;
			group.totalBytesCompleted = group.index.incomingCompleted + group.index.outgoingCompleted;
			group.totalCountCompleted = group.index.incomingCountCompleted + group.index.outgoingCountCompleted;

			group.totalFiles = getContext().getResources().getQuantityString(R.plurals.text_files, group.totalCount, group.totalCount);
			group.totalFilesCompleted = getContext().getResources().getQuantityString(R.plurals.text_files, group.totalCountCompleted, group.totalCountCompleted);
			group.totalSize = FileUtils.sizeExpression(group.totalBytes, false);
			group.totalPercent = group.totalBytesCompleted == 0 || group.totalBytes == 0
					? 0.0 : Long.valueOf(group.totalBytesCompleted).doubleValue() / Long.valueOf(group.totalBytes).doubleValue();

			lister.offer(group);
		}
	}

	@Override
	protected PreloadedGroup onGenerateRepresentative(String representativeText)
	{
		return new PreloadedGroup(representativeText);
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public TransactionGroupListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	@NonNull
	@Override
	public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		if (viewType == VIEW_TYPE_REPRESENTATIVE)
			return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

		return new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(R.layout.list_transaction_group, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
	{
		try {
			final PreloadedGroup object = getItem(position);

			if (!holder.tryBinding(object)) {
				final View parentView = holder.getView();
				int appliedColorRes;

				ImageView image = parentView.findViewById(R.id.image);
				TextView text1 = parentView.findViewById(R.id.text);
				TextView text2 = parentView.findViewById(R.id.text2);
				TextView text3 = parentView.findViewById(R.id.text3);

				parentView.setSelected(object.isSelectableSelected());

				if (object.index.hasIssues)
					appliedColorRes = R.color.errorTintColor;
				else
					appliedColorRes = object.totalCount == object.totalCountCompleted
							? R.color.colorAccent : R.color.layoutTintLightColor;

				if (object.isRunning) {
					image.setImageResource(R.drawable.ic_pause_white_24dp);
				} else {
					if ((object.index.outgoingCount == 0 && object.index.incomingCount == 0)
							|| (object.index.outgoingCount > 0 && object.index.incomingCount > 0))
						image.setImageResource(object.index.outgoingCount > 0
								? R.drawable.ic_compare_arrows_white_24dp
								: R.drawable.ic_error_white_24dp);
					else
						image.setImageResource(object.index.outgoingCount > 0
								? R.drawable.ic_file_upload_black_24dp
								: R.drawable.ic_file_download_black_24dp);
				}

				@ColorInt
				int appliedColor = ContextCompat.getColor(getContext(), appliedColorRes);

				ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor));
				text1.setTextColor(appliedColor);
				text1.setText(getContext().getString(R.string.mode_itemCountedDetailed, object.assignees, object.totalSize));
				text2.setText(mPercentFormat.format(object.totalPercent));
				text3.setText(object.totalFiles);
			}
		} catch (Exception e) {
		}
	}

	public void updateActiveList(long[] activeList)
	{
		synchronized (mRunningTasks) {
			mRunningTasks.clear();

			for (long groupId : activeList)
				mRunningTasks.add(groupId);
		}
	}

	public static class PreloadedGroup
			extends TransferGroup
			implements GroupEditableListAdapter.GroupEditable
	{
		public int viewType;
		public String representativeText;

		public Index index = new Index();
		public String assignees;
		public String totalSize;
		public String totalFiles;
		public String totalFilesCompleted;

		public int totalCount;
		public int totalCountCompleted;
		public long totalBytes;
		public long totalBytesCompleted;
		public double totalPercent;
		public boolean isRunning;

		public PreloadedGroup()
		{
		}

		public PreloadedGroup(String representativeText)
		{
			this.viewType = TransactionGroupListAdapter.VIEW_TYPE_REPRESENTATIVE;
			this.representativeText = representativeText;
		}

		@Override
		public String getComparableName()
		{
			return getSelectableTitle();
		}

		@Override
		public long getComparableDate()
		{
			return dateCreated;
		}

		@Override
		public long getComparableSize()
		{
			return totalCount;
		}

		@Override
		public long getId()
		{
			return groupId;
		}

		@Override
		public String getSelectableTitle()
		{
			return totalFiles + " (" + totalSize + ")";
		}

		@Override
		public int getViewType()
		{
			return viewType;
		}

		@Override
		public String getRepresentativeText()
		{
			return representativeText;
		}

		@Override
		public boolean isGroupRepresentative()
		{
			return representativeText != null;
		}

		@Override
		public void setDate(long date)
		{
			this.dateCreated = date;
		}

		@Override
		public void setId(long id)
		{
			this.groupId = id;
		}

		@Override
		public boolean setSelectableSelected(boolean selected)
		{
			return !isGroupRepresentative() && super.setSelectableSelected(selected);
		}

		@Override
		public void setSize(long size)
		{
			this.totalCount = ((Long) size).intValue();
		}

		@Override
		public void setRepresentativeText(CharSequence representativeText)
		{
			this.representativeText = String.valueOf(representativeText);
		}
	}
}
