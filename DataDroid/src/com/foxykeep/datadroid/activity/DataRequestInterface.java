package com.foxykeep.datadroid.activity;

import android.os.Bundle;

public interface DataRequestInterface {
	public abstract void onRequestFinishedError(int workerType, Bundle payload);
	public abstract void onRequestFinishedSuccess(int workerType, Bundle payload);
	public abstract boolean isPostRequest(int workerType);
}

