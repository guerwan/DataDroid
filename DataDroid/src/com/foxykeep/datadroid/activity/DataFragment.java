package com.foxykeep.datadroid.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class DataFragment extends Fragment
	implements DataRequestInterface{
	
	private DataRequestListener mRequestListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRequestListener = new DataRequestListener(getActivity(), savedInstanceState, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mRequestListener.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mRequestListener.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mRequestListener.onPause();
	}
	

	protected void loadRequest(int workerType, Bundle bundle)
	{
		loadRequest(workerType, bundle, false);
	}
	
	protected void loadRequest(int workerType, Bundle bundle, boolean forceFromDB)
	{
		mRequestListener.loadRequest(workerType, bundle, forceFromDB, saveInSoftMemoryRequest(workerType));
	}
	
	protected void invalidateRequest(int workerType)
	{
		mRequestListener.removeRequestByType(workerType, true);
	}
	
}
