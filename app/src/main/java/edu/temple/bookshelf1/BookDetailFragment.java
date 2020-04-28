package edu.temple.bookshelf1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.Serializable;
import java.util.HashMap;

import edu.temple.audiobookplayer.AudiobookService;
import edu.temple.audiobookplayer.AudiobookService.BookProgress;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BookDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@SuppressLint("ParcelCreator")
public class BookDetailFragment extends Fragment implements Parcelable {

    private static final String BOOK_KEY = "book";
    private static final String MEDIA_CONTROLLER = "mc";

    private Book book;

    TextView titleTextView, authorTextView;
    ImageView coverImageView;
    Button playButton;

    public BookDetailFragment() {}


    public static BookDetailFragment newInstance(Book book) {
        BookDetailFragment fragment = new BookDetailFragment();
        Bundle args = new Bundle();

        /*
         A HashMap implements the Serializable interface
         therefore we can place a HashMap inside a bundle
         by using that put() method.
         */
        args.putParcelable(BOOK_KEY, (Parcelable) book);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            book = (Book) getArguments().getParcelable(BOOK_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_details, container, false);

        titleTextView = v.findViewById(R.id.titleTextView);
        authorTextView = v.findViewById(R.id.authorTextView);
        coverImageView = v.findViewById(R.id.coverImageView);
        playButton = v.findViewById(R.id.playButton);


        /*
        Because this fragment can be created with or without
        a book to display when attached, we need to make sure
        we don't try to display a book if one isn't provided
         */

        if (book != null)
            displayBook(book);

        return v;
    }

    /*
    This method is used both internally and externally (from the activity)
    to display a book
     */
    public void displayBook(Book book) {
        titleTextView.setText(book.title);
        authorTextView.setText(book.author);

        String url = book.coverURL.toString();
        Context context = this.getActivity();
        Picasso.with(context).load(url).into(coverImageView);

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(book, flags);
    }

}
