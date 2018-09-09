package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.app.EditableListFragmentModelImpl;
import com.genonbeta.TrebleShot.fragment.ApplicationListFragment;
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment;
import com.genonbeta.TrebleShot.fragment.ImageListFragment;
import com.genonbeta.TrebleShot.fragment.MusicListFragment;
import com.genonbeta.TrebleShot.fragment.VideoListFragment;
import com.genonbeta.TrebleShot.ui.callback.SharingActionModeCallback;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

/**
 * created by: veli
 * date: 13/04/18 19:45
 * p.s.: if anyone could do all this without making them generified I would be much surprised.
 */
public class ContentSharingActivity extends Activity
{
	public static final String TAG = ContentSharingActivity.class.getSimpleName();

	private PowerfulActionMode mMode;
	private SharingActionModeCallback mSelectionCallback;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content_sharing);

		final AppBarLayout appBarLayout = findViewById(R.id.app_bar);
		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mMode = findViewById(R.id.activity_content_sharing_action_mode);
		final TabLayout tabLayout = findViewById(R.id.activity_content_sharing_tab_layout);
		final ViewPager viewPager = findViewById(R.id.activity_content_sharing_view_pager);

		mSelectionCallback = new SharingActionModeCallback(null);
		final PowerfulActionMode.SelectorConnection selectorConnection = new PowerfulActionMode.SelectorConnection(mMode, mSelectionCallback);

		final EditableListFragment.LayoutClickListener groupLayoutClickListener
				= new EditableListFragment.LayoutClickListener()
		{
			@Override
			public boolean onLayoutClick(EditableListFragment listFragment, EditableListAdapter.EditableViewHolder holder, boolean longClick)
			{
				if (longClick)
					return listFragment.onDefaultClickAction(holder);

				return selectorConnection.setSelected(holder);
			}
		};

		final SmartFragmentPagerAdapter pagerAdapter = new SmartFragmentPagerAdapter(this, getSupportFragmentManager())
		{
			@Override
			public void onItemInstantiated(StableItem item)
			{
				EditableListFragmentImpl fragmentImpl = (EditableListFragmentImpl) item.getInitiatedItem();
				EditableListFragmentModelImpl fragmentModelImpl = (EditableListFragmentModelImpl) item.getInitiatedItem();

				fragmentImpl.setSelectionCallback(mSelectionCallback);
				fragmentImpl.setSelectorConnection(selectorConnection);
				fragmentModelImpl.setLayoutClickListener(groupLayoutClickListener);

				if (viewPager.getCurrentItem() == item.getCurrentPosition())
					mSelectionCallback.updateProvider(fragmentImpl);
			}
		};

		mMode.setContainerLayout(findViewById(R.id.activity_content_sharing_action_mode_layout));

		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(0, ApplicationListFragment.class, null));
		pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(1, FileExplorerFragment.class, null)
				.setTitle(getString(R.string.text_files)));
		pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(2, MusicListFragment.class, null));
		pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(3, ImageListFragment.class, null));
		pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(4, VideoListFragment.class, null));

		pagerAdapter.createTabs(tabLayout);
		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
		{
			@Override
			public void onTabSelected(TabLayout.Tab tab)
			{
				viewPager.setCurrentItem(tab.getPosition());

				final EditableListFragment fragment = (EditableListFragment) pagerAdapter.getItem(tab.getPosition());

				mSelectionCallback.updateProvider(fragment);

				if (fragment.getAdapterImpl() != null)
					new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							fragment.getAdapterImpl().notifyAllSelectionChanges();
						}
					}, 200);
			}

			@Override
			public void onTabUnselected(final TabLayout.Tab tab)
			{

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab)
			{

			}
		});

		mMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
		{
			@Override
			public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
			{
				appBarLayout.setExpanded(!started, true);
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == android.R.id.home)
			onBackPressed();
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	public void onBackPressed()
	{
		if (mMode != null
				&& mSelectionCallback != null
				&& mMode.hasActive(mSelectionCallback))
			mMode.finish(mSelectionCallback);
		else
			super.onBackPressed();
	}
}
