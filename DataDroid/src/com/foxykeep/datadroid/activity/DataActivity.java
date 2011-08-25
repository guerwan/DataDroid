package com.foxykeep.datadroid.activity;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;

import com.foxykeep.datadroid.requestmanager.RequestManager;
import com.foxykeep.datadroid.requestmanager.RequestManager.OnRequestFinishedListener;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestState;
import com.foxykeep.datadroid.service.WorkerService;

public abstract class DataActivity extends Activity 
	implements OnRequestFinishedListener{
	private static final int MAX_REQUEST_TYPES = 20;
	private static final String SAVED_STATE_REQUEST_IDS = "savedStateRequestIds";
	private static final String SAVED_STATE_REQUEST_TYPES = "savedStateRequestTypes";
	private static final String SAVED_STATE_REQUEST_BUNDLES = "savedStateRequestBundles";
	private static final String SAVED_STATE_FIRST_LAUNCH = "savedStateFirstLaunch";

	protected RequestManager mRequestManager;
	// Stores RequestIds and RequestTypes
	protected ArrayList<Integer> mRequestTypes;
	protected ArrayList<Integer> mRequestIds;
	protected ArrayList<Bundle> mRequestBundles;


	protected boolean mFirstLaunch = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRequestManager = RequestManager.from(this);
		mRequestTypes = new ArrayList<Integer>(MAX_REQUEST_TYPES);
		mRequestIds = new ArrayList<Integer>(MAX_REQUEST_TYPES);
		mRequestBundles = new ArrayList<Bundle>(MAX_REQUEST_TYPES);
		
		if(savedInstanceState != null)
		{
			mRequestIds = savedInstanceState.getIntegerArrayList(SAVED_STATE_REQUEST_IDS);
			mRequestTypes = savedInstanceState.getIntegerArrayList(SAVED_STATE_REQUEST_TYPES);
			mRequestBundles = savedInstanceState.getParcelableArrayList(SAVED_STATE_REQUEST_BUNDLES);
			mFirstLaunch = savedInstanceState.getBoolean(SAVED_STATE_FIRST_LAUNCH);
			restoreInstanceState(savedInstanceState);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntegerArrayList(SAVED_STATE_REQUEST_IDS, mRequestIds);
		outState.putIntegerArrayList(SAVED_STATE_REQUEST_TYPES, mRequestTypes);
		outState.putParcelableArrayList(SAVED_STATE_REQUEST_BUNDLES, mRequestBundles);
		outState.putBoolean(SAVED_STATE_FIRST_LAUNCH, mFirstLaunch);
	}

	protected abstract void restoreInstanceState(Bundle savedInstanceState);
	protected abstract void onActivityCreated();


	@Override
	protected void onResume() {
		super.onResume();
		if (mRequestIds.size() != 0) {
			int requestType;
			Integer requestId;
			for(int i = 0; i<mRequestIds.size(); i++)
			{
				requestId = mRequestIds.get(i);
				if(requestId != null)
				{
					requestType = mRequestTypes.get(i);
					loadRequest(requestType, mRequestBundles.get(i));
				}
			}
		}
		else if (mFirstLaunch)
		{
			onActivityCreated();
			mFirstLaunch = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mRequestIds.size() != 0) {
			Integer requestId;
			for(int i = 0; i<mRequestIds.size(); i++)
			{
				requestId = mRequestIds.get(i);
				if(requestId != null)
					mRequestManager.removeOnRequestFinishedListener(requestId);
			}
		}
	}


	@Override
	public void onRequestFinished(int requestId, int resultCode, Bundle payload) {

		mRequestManager.removeOnRequestFinishedListener(requestId);
		int index = mRequestIds.indexOf(requestId);
		int requestType = mRequestTypes.get(index);
		
		if (resultCode == WorkerService.ERROR_CODE) {
			onRequestFinishedError(requestType, payload);
		} else {
			onRequestFinishedSuccess(requestType, payload);
		}
	}

	protected abstract void onRequestFinishedError(int requestType, Bundle payload);
	protected abstract void onRequestFinishedSuccess(int requestType, Bundle payload);


	protected void addRequest(int requestId, int requestType, Bundle bundle)
	{
		int index = mRequestTypes.indexOf(requestType); 
		if(index != -1)
		{	
			mRequestIds.remove(index);
			mRequestIds.add(index, requestId);
			mRequestBundles.remove(index);
			mRequestBundles.add(index, bundle);
		}
		else
		{
			mRequestTypes.add(requestType);
			mRequestIds.add(requestId);
			mRequestBundles.add(bundle);
		}
	}
	
	protected void removeRequestFromType(int requestType)
	{
		int index = mRequestTypes.indexOf(requestType); 
		if(index != -1)
		{	
			mRequestIds.remove(index);
			mRequestTypes.remove(index);
			mRequestBundles.remove(index);
		}
	}
	
	protected void removeRequestFromId(int requestId)
	{
		int index = mRequestIds.indexOf(requestId); 
		if(index != -1)
		{	
			mRequestTypes.remove(index);
			mRequestIds.remove(index);
			mRequestBundles.remove(index);
		}
	}
	
	protected RequestState isRequestLoaded(int requestType)
	{
		int index = mRequestTypes.indexOf(requestType);
		if(index != -1)
		{
			// The request has been launched if it is not in progress it is already loaded
			if(mRequestManager.isRequestInProgress(mRequestIds.get(index)))
				return RequestState.RUNNING;
			else
				return RequestState.LOADED;
		}
		else
			return RequestState.NOT_LAUNCHED;
	}
	
	protected void loadRequest(int requestType, Bundle bundle)
	{
		switch(isRequestLoaded(requestType))
		{
		case LOADED:
			launchRequestOnDB(requestType, bundle);
			break;
		case NOT_LAUNCHED:
			launchRequest(requestType, bundle);
			break;
		case RUNNING:
			int index = mRequestTypes.indexOf(requestType);
			mRequestManager.addOnRequestFinishedListener(mRequestIds.get(index), this);
			break;
		}	
	}
	
	protected abstract int getWorkerType(int requestType, boolean fromDb);
	
	protected void launchRequest(int requestType, Bundle bundle){
		
		int workerType = getWorkerType(requestType, false);
		int requestId = mRequestManager.request(workerType, 
				this, bundle);
		
		if(requestId != -1)
			addRequest(requestId, requestType, bundle);
	}
	
	protected void launchRequestOnDB(int requestType, Bundle bundle){
		
			int workerType = getWorkerType(requestType, true);
			int requestId = mRequestManager.request(workerType, 
					this, bundle);
			
			if(requestId != -1)
				addRequest(requestId, requestType, bundle);
	}
}
