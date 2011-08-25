package com.foxykeep.datadroid.requestmanager;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Request implements Parcelable{
	public int type;
	public int id;
	public Bundle bundle;

	public Request(int id, int type, Bundle bundle) {
        this.id = id;
        this.type = type;
        this.bundle = bundle;
    }
	
	private Request(final Parcel in) {
        id = in.readInt();
    	type = in.readInt();
    	bundle = in.readBundle();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel dest, final int flags) {
    	dest.writeInt(id);
        dest.writeInt(type);
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
        public Request createFromParcel(final Parcel in) {
            return new Request(in);
        }

        public Request[] newArray(final int size) {
            return new Request[size];
        }
    };
}
