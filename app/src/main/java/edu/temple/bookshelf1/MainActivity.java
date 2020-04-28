package edu.temple.bookshelf1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import edu.temple.audiobookplayer.AudiobookService;
import edu.temple.audiobookplayer.AudiobookService.BookProgress;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, Button.OnClickListener {

    FragmentManager fm;
    boolean twoPane;
    BookDetailFragment bookDetailsFragment;
    BookListFragment bookListFragment;
    Button search;
    RequestQueue requestQueue;
    ArrayList<Book> books;
    int bookChosen;
    int bookChosenId;
    String bookPlaying;

    SeekBar seekBar;
    Button playButton;
    Button pauseButton;
    Button rewindButton;
    int seekBarProgress;
    int playingMax;
    private Handler handler;
    private Runnable runnable;

    Intent audioServiceIntent;
    boolean connected;
    AudiobookService audiobookService;
    AudiobookService.MediaControlBinder audiobookServiceBinder;
    ServiceConnection serviceConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connected = true;
            audiobookServiceBinder = (AudiobookService.MediaControlBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
            audiobookService = null;
            audiobookServiceBinder = null;
        }
    };;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new NukeSSLCerts().nuke();
        requestQueue = Volley.newRequestQueue(getApplicationContext());


        // Set initial state by declaring books, search button listener, and whether a two pane display
        books = new ArrayList<>();
        bookChosen = -1;
        search = findViewById(R.id.searchButton);
        search.setOnClickListener(this);
        playButton = findViewById(R.id.playButton);
        seekBar = findViewById(R.id.seekBar);
        pauseButton = findViewById(R.id.pauseButton);
        rewindButton = findViewById(R.id.rewindButton);
        handler = new Handler();
        hidePlayFeatures();
        twoPane = findViewById(R.id.container2) != null;
        audioServiceIntent = new Intent(MainActivity.this, AudiobookService.class);
        bindService(audioServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        // Display book list before anything is searched
        fm = getSupportFragmentManager();
        if(savedInstanceState != null) {
            books = (ArrayList<Book>) savedInstanceState.getSerializable("books");
            bookListFragment = savedInstanceState.getParcelable("fragment1");
            bookDetailsFragment = (BookDetailFragment) savedInstanceState.getParcelable("fragment2");
            showPlayFeatures();
            bookChosen = savedInstanceState.getInt("index");
            if(bookChosen != -1) {
                bookChosenId = (books.get(bookChosen)).id;
            }

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
                bookChosenId= (books.get(bookChosen)).id;
                seekBar.setMax((int) books.get(bookChosen).duration);
                bookDetailsFragment = BookDetailFragment.newInstance(books.get(bookChosen));
                fm.beginTransaction()
                        .replace(R.id.container1, bookDetailsFragment)
                        .addToBackStack(null)
                        .commit();
                prepareForReception();
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
        bookChosenId = (books.get(index)).id;
        showPlayFeatures();
        if (twoPane) {
            bookDetailsFragment.displayBook(books.get(index));
        }

        else {
            bookChosenId = books.get(index).id;
            bookDetailsFragment = BookDetailFragment.newInstance(books.get(index));
            fm.beginTransaction()
                    .replace(R.id.container1, bookDetailsFragment)
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
        prepareForReception();
    }


    public void prepareForReception() {
            if(playingMax != seekBar.getMax()) {
                seekBar.setMax(books.get(bookChosen).duration);
            }
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookPlaying = books.get(bookChosen).title;
                Toast.makeText(MainActivity.this, ("Now playing ") + bookPlaying, Toast.LENGTH_LONG*10).show();
                if(audiobookServiceBinder.isPlaying() == false) {
                    audiobookServiceBinder.play(bookChosenId, seekBarProgress);
                    playingMax = books.get(bookChosen).duration;
                }
                if(audiobookServiceBinder.isPlaying() == true && (seekBar.getMax() == playingMax)) {
                    audiobookServiceBinder.pause();
                }
                else {
                    audiobookServiceBinder.play(bookChosenId, seekBarProgress);
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected) {
                    if (audiobookServiceBinder.isPlaying() == true) {
                        audiobookServiceBinder.pause();
                        seekBarProgress = seekBar.getProgress();
                    } else {
                        audiobookServiceBinder.play(bookChosenId, seekBarProgress);
                    }
                }
            }
        });

        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected) {
                    seekBarProgress = 0;
                    seekBar.setProgress(seekBarProgress);
                    audiobookServiceBinder.seekTo(0);
                }
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (connected) {
                    seekBarProgress = progress;
                    audiobookServiceBinder.play(bookChosenId, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public void hidePlayFeatures() {
        if(twoPane == false) {
            seekBar.setVisibility(View.INVISIBLE);
            playButton.setVisibility(View.INVISIBLE);
            pauseButton.setVisibility(View.INVISIBLE);
            rewindButton.setVisibility(View.INVISIBLE);
        }
    }

    public void showPlayFeatures() {
        playButton.setVisibility(View.VISIBLE);
        seekBar.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.VISIBLE);
        rewindButton.setVisibility(View.VISIBLE);
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
                        book.setDuration(bookDetails.getInt("duration"));
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
