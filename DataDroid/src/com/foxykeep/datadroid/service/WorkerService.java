/*
 * 2011 Foxykeep (http://datadroid.foxykeep.com)
 *
 * Licensed under the Beerware License :
 * 
 *   As long as you retain this notice you can do whatever you want with this stuff. If we meet some day, and you think
 *   this stuff is worth it, you can buy me a beer in return
 */
package com.foxykeep.datadroid.service;


import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.foxykeep.datadroid.config.LogConfig;
import com.foxykeep.datadroid.requestmanager.RequestManager;

/**
 * This class is the superclass of all the worker service you'll create.
 * 
 * @author Foxykeep
 */
abstract public class WorkerService extends MultiThreadService {
	// Max number of parallel threads used
	private static final int MAX_THREADS = 5;
    
	public static final String LOG_TAG = WorkerService.class.getSimpleName();
    
    public static final String INTENT_ACTION = "com.foxykeep.datadroid.DataService";
    
    public static final String INTENT_EXTRA_WORKER_TYPE = "com.foxykeep.datadroid.extras.workerType";
    public static final String INTENT_EXTRA_FROM_DB = "com.foxykeep.datadroid.extras.fromDB";
    public static final String INTENT_EXTRA_IS_POST_REQUEST = "com.foxykeep.datadroid.extras.postRequest";
    public static final String INTENT_EXTRA_SAVE_IN_MEMORY = "com.foxykeep.datadroid.extras.saveInMemory";
    public static final String INTENT_EXTRA_REQUEST_ID = "com.foxykeep.datadroid.extras.requestId";
    public static final String INTENT_EXTRA_PACKAGE_NAME = "com.foxykeep.datadroid.extras.packageName";
    public static final String INTENT_EXTRA_RECEIVER = "com.foxykeep.datadroid.extras.receiver";

    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = -1;
   
    
    public WorkerService() {
        super(MAX_THREADS);
    }

    /**
     * Proxy method for {@link #sendResult(Intent, Bundle, int)} when the work
     * is a success
     * 
     * @param intent The value passed to {@link onHandleIntent(Intent)}.
     * @param data A {@link Bundle} with the data to send back
     */
    protected void sendSuccess(final Intent intent, final Bundle data) {
        sendResult(intent, data, SUCCESS_CODE);
    }

    /**
     * Proxy method for {@link #sendResult(Intent, Bundle, int)} when the work
     * is a failure
     * 
     * @param intent The value passed to {@link onHandleIntent(Intent)}.
     * @param data A {@link Bundle} the data to send back
     */
    protected void sendFailure(final Intent intent, final Bundle data) {
        sendResult(intent, data, ERROR_CODE);
    }

    /**
     * Proxy method for {@link #sendResult(Intent, Bundle, int)} when the work
     * is a failure due to the network
     * 
     * @param intent The value passed to {@link onHandleIntent(Intent)}.
     * @param data A {@link Bundle} the data to send back
     */
    protected void sendConnexionFailure(final Intent intent, Bundle data) {
        if (data == null) {
            data = new Bundle();
        }
        data.putInt(RequestManager.RECEIVER_EXTRA_ERROR_TYPE, RequestManager.RECEIVER_EXTRA_VALUE_ERROR_TYPE_CONNEXION);
        sendResult(intent, data, ERROR_CODE);
    }

    /**
     * Proxy method for {@link #sendResult(Intent, Bundle, int)} when the work
     * is a failure due to the data (parsing for example)
     * 
     * @param intent The value passed to {@link onHandleIntent(Intent)}.
     * @param data A {@link Bundle} the data to send back
     */
    protected void sendDataFailure(final Intent intent, Bundle data) {
        if (data == null) {
            data = new Bundle();
        }
        data.putInt(RequestManager.RECEIVER_EXTRA_ERROR_TYPE, RequestManager.RECEIVER_EXTRA_VALUE_ERROR_TYPE_DATA);
        sendResult(intent, data, ERROR_CODE);
    }

    /**
     * Method used to send back the result to the {@link RequestManager}
     * 
     * @param intent The value passed to {@link onHandleIntent(Intent)}. It must
     *            contain the {@link ResultReceiver} and the requestId
     * @param data A {@link Bundle} the data to send back
     * @param code The success/error code to send back
     */
    protected void sendResult(final Intent intent, Bundle data, final int code) {

        if (LogConfig.DP_DEBUG_LOGS_ENABLED) {
            Log.d(LOG_TAG, "sendResult : " + ((code == SUCCESS_CODE) ? "Success" : "Failure"));
        }

        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra(INTENT_EXTRA_RECEIVER);

        if (receiver != null) {
        	Bundle result = null;
        	// Also adding the request parameters (can be useful when receiving the result)
            if(intent != null && intent.getExtras() != null)
            {
            	result = intent.getExtras();
            	result.putAll(data);
            } 
            	
            if (result == null) {
            	result = new Bundle();
            }

            result.putInt(RequestManager.RECEIVER_EXTRA_REQUEST_ID, intent.getIntExtra(INTENT_EXTRA_REQUEST_ID, -1));
            
            result.putBoolean(RequestManager.RECEIVER_EXTRA_REQUEST_SAVE_IN_MEMORY, 
            		intent.getBooleanExtra(INTENT_EXTRA_IS_POST_REQUEST, false) || 
            			intent.getBooleanExtra(INTENT_EXTRA_SAVE_IN_MEMORY, false));
            
            result.putInt(RequestManager.RECEIVER_EXTRA_RESULT_CODE, code);

            receiver.send(code, result);
        }
    }
    
    @Override
	protected void onHandleIntent(final Intent intent) {
    	String packageName = intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME);
    	if(!packageName.equals(this.getPackageName()))
    		return;
    	boolean fromDB = intent.getBooleanExtra(INTENT_EXTRA_FROM_DB, false);
    	handleIntent(intent, fromDB);
    	
    }
    
    protected abstract void handleIntent(Intent intent, boolean fromDB);
   
}
