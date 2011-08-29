package com.foxykeep.datadroid.activity;

import android.os.Bundle;

public interface DataRequestInterface {
	public abstract void onRequestFinishedError(int requestType, Bundle payload);
	public abstract void onRequestFinishedSuccess(int requestType, Bundle payload);
	public abstract void restoreInstanceState(Bundle savedInstanceState);
	public abstract int getWorkerType(int requestType, boolean fromDb);

}

