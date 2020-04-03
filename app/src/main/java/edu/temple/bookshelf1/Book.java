package edu.temple.bookshelf1;

import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {
    int id;
    String title;
    String author;
    String coverURL;

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCoverURL(String coverURL) {
        this.coverURL = coverURL;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(int id) {
        this.id = id;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
