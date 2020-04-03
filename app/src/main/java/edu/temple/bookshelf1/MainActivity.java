package edu.temple.bookshelf1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, Button.OnClickListener {

    FragmentManager fm;
    boolean twoPane;
    BookDetailFragment bookDetailsFragment;
    BookListFragment bookListFragment;
    Button search;
    RequestQueue requestQueue;
    ArrayList<Book> books;
    int bookChosen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new NukeSSLCerts().nuke();
        requestQueue = Volley.newRequestQueue(getApplicationContext());


        // Set initial state by declaring books, search button listener, and whether a two pane display
        books = new ArrayList<>();
        bookChosen = -1;
        search = findViewById(R.id.button);
        search.setOnClickListener(this);
        twoPane = findViewById(R.id.container2) != null;

        // Display book list before anything is searched
        fm = getSupportFragmentManager();
        if(savedInstanceState != null) {
            books = (ArrayList<Book>) savedInstanceState.getSerializable("books");
            bookListFragment = savedInstanceState.getParcelable("fragment1");
            bookDetailsFragment = (BookDetailFragment) savedInstanceState.getParcelable("fragment2");
            bookChosen = savedInstanceState.getInt("index");
            System.out.println(bookChosen);
        }
        else {
            bookListFragment = BookListFragment.newInstance(books);
        }
        fm.beginTransaction()
                .replace(R.id.container1, bookListFragment)
                .addToBackStack(null)
                .commit();


        // Load single instance of BookDetailFragment if in two pane
        if (twoPane) {
            if(savedInstanceState != null) {
                if(bookDetailsFragment == null) {
                    bookDetailsFragment = new BookDetailFragment();
                }
            }
            else {
                bookDetailsFragment = new BookDetailFragment();
            }
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment)
                    .commit();
        }

        else {

            if(bookChosen != -1) {
                bookDetailsFragment = BookDetailFragment.newInstance(books.get(bookChosen));
                fm.beginTransaction()
                        .replace(R.id.container1, bookDetailsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("books", books);
        outState.putInt("index", bookChosen);
        outState.putParcelable("fragment1", bookListFragment);
        outState.putParcelable("fragment2", bookDetailsFragment);
    }

    // When book is selected, create a BookDetailsFragment to showcase book. Use search boolean to pick from list

    @Override
    public void bookSelected(int index) {
        bookChosen = index;
        if (twoPane) {
            bookDetailsFragment.displayBook(books.get(index));
        }

        else {
            bookDetailsFragment = BookDetailFragment.newInstance(books.get(index));
            fm.beginTransaction()
                    .replace(R.id.container1, bookDetailsFragment)
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }


    //When search is clicked, convert search value to String and generate new book list
    @Override
    public void onClick(View v) {
        EditText searchValue = findViewById(R.id.bookSearch);
        String valueEntered = searchValue.getText().toString();
        String url = "https://kamorris.com/lab/abp/booksearch.php?search="+valueEntered;


        final JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    books = new ArrayList<>();
                    for(int i = 0; i < response.length(); i++) {
                        JSONObject bookDetails = response.getJSONObject(i);
                        Book book = new Book();
                        book.setAuthor(bookDetails.getString("author"));
                        book.setTitle(bookDetails.getString("title"));
                        book.setCoverURL(bookDetails.getString("cover_url"));
                        book.setId(bookDetails.getInt("book_id"));
                        books.add(book);
                    }
                        bookListFragment = BookListFragment.newInstance(books);
                        fm.beginTransaction()
                                .replace(R.id.container1, bookListFragment)
                                .addToBackStack(null)
                                .commit();

                        if(bookListFragment.adapter != null) {
                            bookListFragment.adapter.notifyDataSetChanged();
                        }



                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    Toast.makeText(MainActivity.this, "TimeOut Error", Toast.LENGTH_LONG).show();
                } else if (error instanceof NoConnectionError) {
                    Toast.makeText(MainActivity.this, "NoConnection Error", Toast.LENGTH_LONG).show();
                } else if (error instanceof AuthFailureError) {
                    Toast.makeText(MainActivity.this, "AuthFailure Error", Toast.LENGTH_LONG).show();
                } else if (error instanceof ServerError) {
                    Toast.makeText(MainActivity.this, "Server Error", Toast.LENGTH_LONG).show();
                } else if (error instanceof NetworkError) {
                    Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_LONG).show();
                } else if (error instanceof ParseError) {
                    Toast.makeText(MainActivity.this, "Parse Error", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }
        });

        complete(jsonArrayRequest);

    }

        public void complete(JsonArrayRequest jsonArrayRequest) {
            requestQueue.add(jsonArrayRequest);
        }
}
