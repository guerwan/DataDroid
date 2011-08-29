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
			requestInterface.restoreInstanceState(savedInstanceState);
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
						loadRequest(request.type, request.bundle);
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

	protected void addRequest(int requestId, int requestType, Bundle bundle, boolean isPostRequest)
	{
		synchronized (mRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mRequests.remove(request);
			}

			Request newRequest = new Request(requestId, requestType, bundle, isPostRequest);
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

	protected void loadRequest(int requestType, Bundle bundle)
	{
		loadRequest(requestType, bundle, false);
	}
	
	protected void loadRequest(int requestType, Bundle bundle, boolean isPostRequest)
	{
		// Multiple Post requests can run at the same time 
		if(isPostRequest)
			launchRequest(requestType, bundle, isPostRequest);
		
		
		switch(getRequestState(requestType))
		{
		case RECEIVED:
		case LOADED:
			launchRequestOnDB(requestType, bundle);
			break;
		case NOT_LAUNCHED:
			launchRequest(requestType, bundle, isPostRequest);
			break;
		case RUNNING:
			Request request = getRequestByType(requestType);
			mRequestManager.addOnRequestFinishedListener(request.id, this);
			break;
		}	
	}

	protected void launchRequest(int requestType, Bundle bundle, boolean isPostRequest){

		int workerType = mDataInterface.getWorkerType(requestType, false);
		int requestId = mRequestManager.request(workerType, 
				this, bundle, isPostRequest);

		if(requestId != -1)
			addRequest(requestId, requestType, bundle, 
					isPostRequest);
	}

	protected void launchRequestOnDB(int requestType, Bundle bundle){

		int workerType = mDataInterface.getWorkerType(requestType, true);
		int requestId = mRequestManager.request(workerType, 
				this, bundle, false);

		if(requestId != -1)
			addRequest(requestId, requestType, bundle, false);
	}
	

}
