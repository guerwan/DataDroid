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
	
	protected void loadRequest(int requestType, Bundle bundle, boolean isPostRequest)
	{
		mRequestListener.loadRequest(requestType, bundle, isPostRequest);
	}
	
	protected void launchRequest(int requestType, Bundle bundle, boolean isPostRequest){
		mRequestListener.launchRequest(requestType, bundle, isPostRequest);
	}
	
	protected void launchRequestOnDB(int requestType, Bundle bundle){
		mRequestListener.launchRequestOnDB(requestType, bundle);
	}
}
