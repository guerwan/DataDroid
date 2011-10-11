package com.foxykeep.datadroid.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public abstract class DataPreferenceActivity extends PreferenceActivity
	implements DataRequestInterface{
	
	private DataRequestListener mRequestListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRequestListener = new DataRequestListener(this, savedInstanceState, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mRequestListener.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mRequestListener.onResume();
	}

	@Override
	protected void onPause() {
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
