package com.foxykeep.datadroid.activity;

import android.app.TabActivity;
import android.os.Bundle;

public abstract class DataTabActivity extends TabActivity
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
		boolean isPost = isPostRequest(workerType);
		mRequestListener.loadRequest(workerType, bundle, isPost);
	}
	
	protected void invalidateRequest(int workerType)
	{
		mRequestListener.removeRequestByType(workerType);
	}
}
