package com.foxykeep.datadroid.activity;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;

import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager;
import com.foxykeep.datadroid.requestmanager.RequestManager.OnRequestFinishedListener;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestState;
import com.foxykeep.datadroid.service.WorkerService;

public class DataRequestListener implements OnRequestFinishedListener {
	
	private static final int MAX_NUMBER_OF_REQUESTS= 20;
	private static final String SAVED_STATE_REQUESTS = "savedStateRequests";
	
	protected RequestManager mRequestManager;
	protected ArrayList<Request> mRequests;
	protected DataRequestInterface mDataInterface;

	public DataRequestListener(Context context, Bundle savedInstanceState, 
			DataRequestInterface requestInterface) {
		mRequestManager = RequestManager.from(context);
		mRequests = new ArrayList<Request>(MAX_NUMBER_OF_REQUESTS);
		mDataInterface = requestInterface;
		
		if(savedInstanceState != null)
		{
			mRequests = savedInstanceState.getParcelableArrayList(SAVED_STATE_REQUESTS);
		}
	}

	
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(SAVED_STATE_REQUESTS, mRequests);
	}

	
	protected void onResume() {
		if (mRequests.size() != 0) {
			Request request;
			for(int i = 0; i<mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null)
				{
					if(request.isPostRequest)
					{
						mRequestManager.getPostResultFromMemory(request.id, this);
					}
					else if (request.state != RequestState.RECEIVED)
						loadRequest(request.type, request.bundle, request.isPostRequest);
				}
			}
		}
	}

	protected void onPause() {
		if (mRequests.size() != 0) {
			Request request;
			for(int i = 0; i<mRequests.size(); i++)
			{
				request = mRequests.get(i);
				
				if(request != null)
					mRequestManager.removeOnRequestFinishedListener(request.id);
			}
		}
	}

	private Request getRequestById(int requestId)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null && request.id == requestId)
					return request;
			}
			return null;
		}
	}

	private Request getRequestByType(int requestType)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null 
						&& request.type == requestType)
					return request;
			}
			return null;
		}
	}


	private boolean removeRequestById(int requestId)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null && request.id == requestId)
				{
					mRequests.remove(i);
					return true;
				}
			}
			return false;
		}
	}
	
	
	//Can be used when an action changes the result of the already executed query
	public boolean removeRequestByType(int workerType)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null && request.type == workerType)
				{
					mRequests.remove(i);
					return true;
				}
			}
			return false;
		}
	}

	public void onRequestFinished(int requestId, int resultCode, Bundle payload) {

		mRequestManager.removeOnRequestFinishedListener(requestId);

		Request request = getRequestById(requestId);

		if (resultCode == WorkerService.ERROR_CODE) {
			mDataInterface.onRequestFinishedError(request.type, payload);
		} else {
			mDataInterface.onRequestFinishedSuccess(request.type, payload);
		}
		
		if(request.isPostRequest)
			removeRequestById(requestId);
		else
			request.state = RequestState.RECEIVED;
	
	}

	protected void addRequest(int requestId, int workerType, Bundle bundle, boolean isPostRequest)
	{
		synchronized (mRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mRequests.remove(request);
			}

			Request newRequest = new Request(requestId, workerType, bundle, isPostRequest);
			mRequests.add(newRequest);	
		}
	}


	protected void removeRequestFromId(int requestId)
	{
		synchronized (mRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mRequests.remove(request);
			}
		}
	}

	protected RequestState getRequestState(int requestType)
	{
		Request request = getRequestByType(requestType);
		if(request != null)
		{
			// We check if request is still running because we are not sure
			// we have received the result
			if(request.state == RequestState.RUNNING && 
					!mRequestManager.isRequestInProgress(request.id))
			{
				request.state = RequestState.LOADED;
			}
			return request.state;
		}
		else  
		{
			return RequestState.NOT_LAUNCHED;
		}
	}

	protected void loadRequest(int workerType, Bundle bundle, 
			boolean isPostRequest)
	{
		loadRequest(workerType, bundle, isPostRequest, false);
	}
	
	protected void loadRequest(int workerType, Bundle bundle, 
			boolean isPostRequest, boolean forceFromDB)
	{
		// Multiple Post requests can run at the same time 
		if(isPostRequest)
		{
			launchRequest(workerType, bundle, forceFromDB, isPostRequest);
			return;
		}
		
		
		switch(getRequestState(workerType))
		{
		case RECEIVED:
		case LOADED:
			launchRequest(workerType, bundle, true, isPostRequest);
			break;
		case NOT_LAUNCHED:
			launchRequest(workerType, bundle, forceFromDB, isPostRequest);
			break;
		case RUNNING:
			Request request = getRequestByType(workerType);
			mRequestManager.addOnRequestFinishedListener(request.id, this);
			break;
		}	
	}

	protected void launchRequest(int workerType, Bundle bundle, boolean fromDB, boolean isPostRequest){

		int requestId = mRequestManager.request(workerType, 
				this, bundle, fromDB, isPostRequest);

		if(requestId != -1)
			addRequest(requestId, workerType, bundle, isPostRequest);
	}
	

}
