/*
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 *
 * Licensed under the Beerware License :
 * 
 *   As long as you retain this notice you can do whatever you want with this stuff. If we meet some day, and you think
 *   this stuff is worth it, you can buy me a beer in return
 */
package com.foxykeep.datadroid.requestmanager;

import java.lang.ref.WeakReference;
import java.util.EventListener;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.SparseArray;

import com.foxykeep.datadroid.service.WorkerService;

/**
 * {@link RequestManager} is the superclass of the classes that will be
 * implemented in your project. It contains constants used in the library.
 * 
 * @author Foxykeep
 */
public class RequestManager {

	public static final String RECEIVER_EXTRA_REQUEST_ID = "com.foxykeep.datadroid.extras.requestId";
	public static final String RECEIVER_EXTRA_REQUEST_IS_POST = "com.foxykeep.datadroid.extras.isPost";
	public static final String RECEIVER_EXTRA_RESULT_CODE = "com.foxykeep.datadroid.extras.code";
	public static final String RECEIVER_EXTRA_PAYLOAD = "com.foxykeep.datadroid.extras.payload";
	public static final String RECEIVER_EXTRA_ERROR_TYPE = "com.foxykeep.datadroid.extras.error";
	public static final int RECEIVER_EXTRA_VALUE_ERROR_TYPE_CONNEXION = 1;
	public static final int RECEIVER_EXTRA_VALUE_ERROR_TYPE_DATA = 2;


	private static final int MAX_RANDOM_REQUEST_ID = 1000000;

	// Singleton management
	protected static RequestManager sInstance;

	public static RequestManager from(final Context context) {
		if (sInstance == null) {
			sInstance = new RequestManager(context);
		} 
		return sInstance;
	}

	private SparseArray<Intent> mRequestSparseArray;
	private Context mContext;
	private SparseArray<WeakReference<OnRequestFinishedListener>> mListenerList;
	private Handler mHandler = new Handler();
	private EvalReceiver mEvalReceiver = new EvalReceiver(mHandler);
	private SparseArray<WeakReference<Bundle>> mPostRequestResultMemory;
	private static Random sRandom = new Random();

	public static enum RequestState {
		NOT_LAUNCHED, 
		RUNNING,
		LOADED,
		RECEIVED}


	protected RequestManager(final Context context) {
		mContext = context.getApplicationContext();
		mRequestSparseArray = new SparseArray<Intent>();
		mListenerList = new SparseArray<WeakReference<OnRequestFinishedListener>>();
		mPostRequestResultMemory = new SparseArray<WeakReference<Bundle>>();
	}

	/**
	 * The ResultReceiver that will receive the result from the Service
	 */
	private class EvalReceiver extends ResultReceiver {
		EvalReceiver(final Handler h) {
			super(h);
		}

		@Override
		public void onReceiveResult(final int resultCode, final Bundle resultData) {
			handleResult(resultCode, resultData);
		}
	}

	/**
	 * Clients may implements this interface to be notified when a request is
	 * finished
	 * 
	 * @author Foxykeep
	 */
	public static interface OnRequestFinishedListener extends EventListener {

		/**
		 * Event fired when a request is finished.
		 * 
		 * @param requestId The request Id (to see if this is the right request)
		 * @param resultCode The result code (0 if there was no error)
		 * @param payload The result of the service execution.
		 */
		public void onRequestFinished(int requestId, int resultCode, Bundle payload);
	}

	/**
	 * Add a {@link OnRequestFinishedListener} to this
	 * {@link RequestManagerHelper}. Clients may use it in order to listen to
	 * events fired when a request is finished.
	 * <p>
	 * <b>Warning !! </b> If it's an {@link Activity} that is used as a
	 * Listener, it must be detached when {@link Activity#onPause} is called in
	 * an {@link Activity}.
	 * </p>
	 * 
	 * @param listener The listener to add to this
	 *            {@link RequestManagerHelper} .
	 */
	public void addOnRequestFinishedListener(int requestId, final OnRequestFinishedListener listener) {
		WeakReference<OnRequestFinishedListener> weakRef = new WeakReference<OnRequestFinishedListener>(listener);
		synchronized (mListenerList) {
			mListenerList.put(requestId, weakRef);
		}
	}

	/**
	 * Remove a {@link OnRequestFinishedListener} to this
	 * {@link RequestManagerHelper}.
	 * 
	 * @param listenerThe listener to remove to this
	 *            {@link RequestManagerHelper}.
	 */
	public void removeOnRequestFinishedListener(int requestId) {
		synchronized (mListenerList) {
			mListenerList.remove(requestId);
		}
	}

	/**
	 * Return whether a request (specified by its id) is still in progress or
	 * not
	 * 
	 * @param requestId The request id
	 * @return whether the request is still in progress or not.
	 */
	public boolean isRequestInProgress(final int requestId) {
		return (mRequestSparseArray.indexOfKey(requestId) >= 0);
	}

	/**
	 * This method is call whenever a request is finished. Call all the
	 * available listeners to let them know about the finished request
	 * 
	 * @param resultCode The result code of the request
	 * @param resultData The bundle sent back by the service
	 */
	protected void handleResult(final int resultCode, final Bundle resultData) {

		// Get the request Id
		final int requestId = resultData.getInt(RECEIVER_EXTRA_REQUEST_ID);
		final boolean isPostRequest = resultData.getBoolean(RECEIVER_EXTRA_REQUEST_IS_POST);

		// Remove the request Id from the "in progress" request list
		mRequestSparseArray.remove(requestId);


		// Call the available listeners
		synchronized (mListenerList) {
			WeakReference<OnRequestFinishedListener> weakRef = mListenerList.get(requestId);
			if (weakRef != null)
			{
				OnRequestFinishedListener listener = weakRef.get();
				if (weakRef != null) {
					listener.onRequestFinished(requestId, resultCode, resultData);
				}
			}
			else if(isPostRequest)
			{
				synchronized (mPostRequestResultMemory) {
					mPostRequestResultMemory.put(requestId, new WeakReference<Bundle>(resultData));
				}
			}
		}
	}


	protected int getRequestIdIfRunning(int workerType, OnRequestFinishedListener listener)
	{
		final int requestSparseArrayLength = mRequestSparseArray.size();
		int requestId = -1;
		for (int i = 0; i < requestSparseArrayLength; i++) {
			final Intent savedIntent = mRequestSparseArray.valueAt(i);

			if (savedIntent.getIntExtra(WorkerService.INTENT_EXTRA_WORKER_TYPE, -1) 
					!= workerType) {
				continue;
			}

			requestId = mRequestSparseArray.keyAt(i);
			addOnRequestFinishedListener(requestId, listener);
			break;
		}

		return requestId;
	}


	protected int manageRequestId(int workerType, 
			OnRequestFinishedListener listener, Bundle extras, boolean isPostRequest)
	{
		int requestId;
		if(!isPostRequest)
		{
			requestId = getRequestIdIfRunning(workerType, listener);
			
			if(requestId != -1)
				return requestId;
		}
		
		requestId = sRandom.nextInt(MAX_RANDOM_REQUEST_ID);
		addOnRequestFinishedListener(requestId, listener);
		

		final Intent intent = new Intent(WorkerService.INTENT_ACTION);
		intent.putExtra(WorkerService.INTENT_EXTRA_WORKER_TYPE, workerType);
		intent.putExtra(WorkerService.INTENT_EXTRA_RECEIVER, mEvalReceiver);
		intent.putExtra(WorkerService.INTENT_EXTRA_REQUEST_ID, requestId);
		intent.putExtra(WorkerService.INTENT_EXTRA_PACKAGE_NAME, mContext.getPackageName());
		intent.putExtras(extras);
		mContext.startService(intent);

		mRequestSparseArray.append(requestId, intent);
		return requestId;
	}

	public int request(int workerType, OnRequestFinishedListener listener, 
			Bundle bundle, boolean isPostRequest) {
		return manageRequestId(workerType, listener, bundle, isPostRequest);
	}

	public void getPostResultFromMemory(int requestId, OnRequestFinishedListener listener) {
		if(isRequestInProgress(requestId))
		{
			addOnRequestFinishedListener(requestId, listener);
		}
		else
		{
			synchronized (mPostRequestResultMemory) {
				WeakReference<Bundle> result = mPostRequestResultMemory.get(requestId);
				if(result != null)
				{
					Bundle bundle = result.get();
					if(bundle != null)
					{
						listener.onRequestFinished(requestId, 
								bundle.getInt(RECEIVER_EXTRA_RESULT_CODE), bundle);
					}
				}
				mPostRequestResultMemory.remove(requestId);
			}
		}
	}
}
