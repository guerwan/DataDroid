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
	private static final String SAVED_STATE_FINISHED_REQUESTS = "savedStateFinishedRequests";


	protected RequestManager mRequestManager;
	// Stores RequestIds and RequestTypes
	protected ArrayList<Request> mRequests;
	protected ArrayList<Request> mFinishedRequests;
	protected DataRequestInterface mDataInterface;
	

	public DataRequestListener(Context context, Bundle savedInstanceState, 
			DataRequestInterface requestInterface) {
		mRequestManager = RequestManager.from(context);
		mRequests = new ArrayList<Request>(MAX_NUMBER_OF_REQUESTS);
		mFinishedRequests = new ArrayList<Request>(MAX_NUMBER_OF_REQUESTS);
		mDataInterface = requestInterface;
		if(savedInstanceState != null)
		{
			mRequests = savedInstanceState.getParcelableArrayList(SAVED_STATE_REQUESTS);
			mFinishedRequests = savedInstanceState.getParcelableArrayList(SAVED_STATE_FINISHED_REQUESTS);
			requestInterface.restoreInstanceState(savedInstanceState);
		}
	}

	
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArrayList(SAVED_STATE_REQUESTS, mRequests);
		outState.putParcelableArrayList(SAVED_STATE_FINISHED_REQUESTS, mFinishedRequests);
	}

	
	protected void onResume() {
		if (mRequests.size() != 0) {
			Request request;
			for(int i = 0; i<mRequests.size(); i++)
			{
				request = mRequests.get(i);
				if(request != null)
				{
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
				// if request not null and request is not Post
				// or you need to store the result of the post request
				if(request != null && !mDataInterface.isPostRequest(request.type))
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
				if(request != null && request.type == requestType)
					return request;
			}
			return null;
		}
	}

	@SuppressWarnings("unused")
	private Request getFinishedRequestById(int requestId)
	{
		synchronized (mFinishedRequests) {
			Request request;
			for(int i = 0; i < mFinishedRequests.size(); i++)
			{
				request = mFinishedRequests.get(i);
				if(request != null && request.id == requestId)
					return request;
			}
			return null;
		}
	}

	private Request getFinishedRequestByType(int requestType)
	{
		synchronized (mFinishedRequests) {
			Request request;
			for(int i = 0; i < mFinishedRequests.size(); i++)
			{
				request = mFinishedRequests.get(i);
				if(request != null && request.type == requestType)
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
		
		removeRequestById(requestId);
		
		if(!mDataInterface.isPostRequest(request.type))
			addFinishedRequest(requestId, request.type, request.bundle);
	}

	protected void addRequest(int requestId, int requestType, Bundle bundle)
	{
		synchronized (mRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mRequests.remove(request);
			}

			Request newRequest = new Request(requestId, requestType, bundle);
			mRequests.add(newRequest);	
		}
	}

	protected void addFinishedRequest(int requestId, int requestType, Bundle bundle)
	{
		synchronized (mFinishedRequests) {
			Request request = getRequestById(requestId);

			if(request != null)
			{	
				mFinishedRequests.remove(request);
			}

			Request newRequest = new Request(requestId, requestType, bundle);
			mFinishedRequests.add(newRequest);
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
			return RequestState.RUNNING;
		else  
		{
			return (getFinishedRequestByType(requestType) != null) ? 
					RequestState.LOADED : RequestState.NOT_LAUNCHED;
		}
	}

	protected void loadRequest(int requestType, Bundle bundle)
	{
		switch(getRequestState(requestType))
		{
		case LOADED:
			launchRequestOnDB(requestType, bundle);
			break;
		case NOT_LAUNCHED:
			launchRequest(requestType, bundle);
			break;
		case RUNNING:
			Request request = getRequestByType(requestType);
			mRequestManager.addOnRequestFinishedListener(request.id, this);
			break;
		}	
	}

	protected void launchRequest(int requestType, Bundle bundle){

		int workerType = mDataInterface.getWorkerType(requestType, false);
		int requestId = mRequestManager.request(workerType, 
				this, bundle);

		if(requestId != -1)
			addRequest(requestId, requestType, bundle);
	}

	protected void launchRequestOnDB(int requestType, Bundle bundle){

		int workerType = mDataInterface.getWorkerType(requestType, true);
		int requestId = mRequestManager.request(workerType, 
				this, bundle);

		if(requestId != -1)
			addRequest(requestId, requestType, bundle);
	}
	

}
