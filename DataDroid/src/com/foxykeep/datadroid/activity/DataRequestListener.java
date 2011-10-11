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
					if(request.state != RequestState.RECEIVED)
					{
						loadRequest(request.type, request.bundle, 
								request.saveInSoftMemory);
					}
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

	private Request getRequestByType(int requestType, Bundle extras)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null 
						&& request.type == requestType)
				{
					boolean extrasMatch = true;
					if(extras != null)
					{
						for(String key : extras.keySet())
						{
							if(!extras.get(key).equals(request.bundle.get(key)))
							{
								extrasMatch = false;
							}
						}
					}
					
					if (extrasMatch)
						return request;
				}
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
	public boolean removeRequestByType(int workerType, boolean removeListener)
	{
		synchronized (mRequests) {
			Request request;
			for(int i = 0; i < mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null && request.type == workerType)
				{
					if(removeListener)
						mRequestManager.removeOnRequestFinishedListener(request.id);
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
		
		if(request.saveInSoftMemory)
			removeRequestById(requestId); // We remove the request if it was saved in soft memory because it can't be fetched from the database anyway
		else
			request.state = RequestState.RECEIVED;
	}

	protected void addRequest(int requestId, int workerType, Bundle bundle, 
			boolean saveInMemory)
	{
		synchronized (mRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mRequests.remove(request);
			}

			Request newRequest = 
					new Request(requestId, workerType, bundle, saveInMemory);
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

	protected RequestState getRequestState(int requestType, Bundle extras)
	{
		Request request = getRequestByType(requestType, extras);
		return getRequestState(request, extras);
	}
	
	protected RequestState getRequestState(Request request, Bundle extras)
	{
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
			boolean saveInMemory)
	{
		loadRequest(workerType, bundle, false, saveInMemory);
	}
	
	protected void loadRequest(int workerType, Bundle bundle, 
			boolean forceFromDB, boolean saveInMemory)
	{
		Request request = getRequestByType(workerType, bundle);
		switch(getRequestState(request, bundle))
		{
		case RECEIVED:
		case LOADED:
			if(saveInMemory)
			{
				mRequestManager.getResultFromSoftMemory(request.id, this);
			}
			else
				launchRequest(workerType, bundle, true, saveInMemory);
			break;
		case NOT_LAUNCHED:
			launchRequest(workerType, bundle, forceFromDB, saveInMemory);
			break;
		case RUNNING:
			mRequestManager.addOnRequestFinishedListener(request.id, this);
			break;
		}	
	}

	protected void launchRequest(int workerType, 
			Bundle bundle, 
			boolean fromDB, 
			boolean saveInMemory){

		int requestId = mRequestManager.request(workerType, 
				this, bundle, fromDB, saveInMemory);

		if(requestId != -1)
			addRequest(requestId, workerType, bundle, saveInMemory);
	}
	

}
