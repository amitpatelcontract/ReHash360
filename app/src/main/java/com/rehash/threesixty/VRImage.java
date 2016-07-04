package com.rehash.threesixty;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**        setContentView(R.layout.vr_image);
 * Created by arms on 6/29/16.
 */
public class VRImage  extends BaseActivity {
    private static final String TAG = VRImage.class.getSimpleName();
    /** Actual panorama widget. **/
    private VrPanoramaView panoWidgetView;
    /**
     * Arbitrary variable to track load status. In this example, this variable should only be accessed
     * on the UI thread. In a real app, this variable would be code that performs some UI actions when
     * the panorama is fully loaded.
     */
    private boolean loadImageSuccessful;
    /** Tracks the file to be loaded across the lifetime of this app. **/
    private Uri fileUri;
    /** Configuration information for the panorama. **/
    private VrPanoramaView.Options panoOptions = new VrPanoramaView.Options();
    private ImageLoaderTask backgroundImageLoaderTask;



    private Button mFacebookLoginButton;
    private Button mGoogleLoginButton;
    private Button mTwitterLoginButton;
    private Button mPasswordLoginButton;
    /**
     * Called when the app is launched via the app icon or an intent using the adb command above. This
     * initializes the app and loads the image to render.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vr_image);

        // Make the source link clickable.
//        TextView sourceText = (TextView) findViewById(R.id.source);
//        sourceText.setText(Html.fromHtml(getString(R.string.source)));
//        sourceText.setMovementMethod(LinkMovementMethod.getInstance());

        panoWidgetView = (VrPanoramaView) findViewById(R.id.pano_view);
        panoWidgetView.setEventListener(new ActivityEventListener());
//        panoWidgetView.setFullscreenButtonEnabled(false);
//        panoWidgetView.setVrModeButtonEnabled(false);


        // Initial launch of the app or an Activity recreation due to rotation.
        handleIntent(getIntent());


//



//        // Set up ListView and Adapter
//        ListView listView = (ListView) findViewById(R.id.list_view);
//
//        MyArrayAdapter adapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_2, CLASSES);
//        adapter.setDescriptionIds(DESCRIPTION_IDS);
//
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check auth on Activity start

    }

    /**
     * Called when the Activity is already running and it's given a new intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, this.hashCode() + ".onNewIntent()");
        // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
        // future invocations.
        setIntent(intent);
        // Load the new image.
        handleIntent(intent);
    }

    /**
     * Load custom images based on the Intent or load the default image. See the Javadoc for this
     * class for information on generating a custom intent via adb.
     */
    private void handleIntent(Intent intent) {
        // Determine if the Intent contains a file to load.
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.i(TAG, "ACTION_VIEW Intent recieved");

            fileUri = intent.getData();
            if (fileUri == null) {
                Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
            } else {
                Log.i(TAG, "Using file " + fileUri.toString());
            }

            panoOptions.inputType = intent.getIntExtra("inputType", VrPanoramaView.Options.TYPE_MONO);
            Log.i(TAG, "Options.inputType = " + panoOptions.inputType);
        } else {
            Log.i(TAG, "Intent is not ACTION_VIEW. Using default pano image.");
            fileUri = null;
            panoOptions.inputType = VrPanoramaView.Options.TYPE_MONO;
        }

        // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
        // take 100s of milliseconds.
        if (backgroundImageLoaderTask != null) {
            // Cancel any task from a previous intent sent to this activity.
            backgroundImageLoaderTask.cancel(true);
        }
        backgroundImageLoaderTask = new ImageLoaderTask();
        backgroundImageLoaderTask.execute(Pair.create(fileUri, panoOptions));
    }

    public boolean isLoadImageSuccessful() {
        return loadImageSuccessful;
    }

    @Override
    protected void onPause() {
        panoWidgetView.pauseRendering();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        panoWidgetView.resumeRendering();
    }

    @Override
    public void onDestroy() {
        panoWidgetView.pauseRendering();
        // Destroy the widget and free memory.
        panoWidgetView.shutdown();

        // The background task has a 5 second timeout so it can potentially stay alive for 5 seconds
        // after the activity is destroyed unless it is explicitly cancelled.
        if (backgroundImageLoaderTask != null) {
            backgroundImageLoaderTask.cancel(true);
        }
        super.onDestroy();
    }
//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        Class clicked = CLASSES[position];
//        startActivity(new Intent(this, clicked));
//    }

    public static class MyArrayAdapter extends ArrayAdapter<Class> {

        private Context mContext;
        private Class[] mClasses;
        private int[] mDescriptionIds;

        public MyArrayAdapter(Context context, int resource, Class[] objects) {
            super(context, resource, objects);

            mContext = context;
            mClasses = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(android.R.layout.simple_list_item_2, null);
            }

            ((TextView) view.findViewById(android.R.id.text1)).setText(mClasses[position].getSimpleName());
            ((TextView) view.findViewById(android.R.id.text2)).setText(mDescriptionIds[position]);

            return view;
        }

        public void setDescriptionIds(int[] descriptionIds) {
            mDescriptionIds = descriptionIds;
        }
    }
    /**
     * Helper class to manage threading.
     */
    class ImageLoaderTask extends AsyncTask<Pair<Uri, VrPanoramaView.Options>, Void, Boolean> {

        /**
         * Reads the bitmap from disk in the background and waits until it's loaded by pano widget.
         */
        @Override
        protected Boolean doInBackground(Pair<Uri, VrPanoramaView.Options>... fileInformation) {
            VrPanoramaView.Options panoOptions = null;  // It's safe to use null VrPanoramaView.Options.
            InputStream istr = null;
            if (fileInformation == null || fileInformation.length < 1
                    || fileInformation[0] == null || fileInformation[0].first == null) {
                AssetManager assetManager = getAssets();
                try {
                    istr = assetManager.open("hema1.jpg");
                    panoOptions = new VrPanoramaView.Options();
                    panoOptions.inputType = VrPanoramaView.Options.TYPE_MONO;
                } catch (IOException e) {
                    Log.e(TAG, "Could not decode default bitmap: " + e);
                    return false;
                }
            } else {
                try {
                    istr = new FileInputStream(new File(fileInformation[0].first.getPath()));
                    panoOptions = fileInformation[0].second;
                } catch (IOException e) {
                    Log.e(TAG, "Could not load file: " + e);
                    return false;
                }
            }

            panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);
            try {
                istr.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close input stream: " + e);
            }

            return true;
        }
    }

    /**
     * Listen to the important events from widget.
     */
    private class ActivityEventListener extends VrPanoramaEventListener {
        /**
         * Called by pano widget on the UI thread when it's done loading the image.
         */
        @Override
        public void onLoadSuccess() {
            loadImageSuccessful = true;
        }

        /**
         * Called by pano widget on the UI thread on any asynchronous error.
         */
        @Override
        public void onLoadError(String errorMessage) {
            loadImageSuccessful = false;
            Toast.makeText(
                    VRImage.this, "Error loading pano: " + errorMessage, Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, "Error loading pano: " + errorMessage);
        }
    }
}

//
//
// extends AppCompatActivity implements View.OnClickListener {//AdapterView.OnItemClickListener {
//
//    private static final Class[] CLASSES = new Class[]{
//            GoogleSignInActivity.class,
//            FacebookLoginActivity.class,
//            EmailPasswordActivity.class,
//            AnonymousAuthActivity.class,
//            CustomAuthActivity.class
//    };
//
//    private static final int[] DESCRIPTION_IDS = new int[]{
//            R.string.desc_google_sign_in,
//            R.string.desc_facebook_login,
//            R.string.desc_emailpassword,
//            R.string.desc_anonymous_auth,
//            R.string.desc_custom_auth,
//    };
//    private Button mFacebookLoginButton;
//    private Button mGoogleLoginButton;
//    private Button mTwitterLoginButton;
//    private Button mPasswordLoginButton;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_chooser);
//        setContentView(R.layout.activity_main);
//
//
//
//        if (HelpUtils.getCurrentUser() != null)
//            HelpUtils.OpenListOfProducts(ChooserActivity.this);
//        mFacebookLoginButton = (Button) findViewById(R.id.login_with_facebook);
//        mGoogleLoginButton = (Button) findViewById(R.id.login_with_google);
////        mTwitterLoginButton = (Button) findViewById(R.id.login_with_twitter);
//        mPasswordLoginButton = (Button) findViewById(R.id.login_with_password);
////        // Set up ListView and Adapter
////        ListView listView = (ListView) findViewById(R.id.list_view);
////
////        MyArrayAdapter adapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_2, CLASSES);
////        adapter.setDescriptionIds(DESCRIPTION_IDS);
////
////        listView.setAdapter(adapter);
////        listView.setOnItemClickListener(this);
//    }
//
//    @Override
//    public void onClick(View v) {
//        final int id = v.getId();
//        Class clicked;
//        switch (id) {
//            case R.id.login_with_facebook:
//                clicked = CLASSES[1];
//                startActivity(new Intent(this, clicked));
//                break;
//            case R.id.login_with_google:
//                clicked = CLASSES[0];
//                startActivity(new Intent(this, clicked));
//                break;
//
//            case R.id.login_with_password:
//                clicked = CLASSES[2];
//                startActivity(new Intent(this, clicked));
//                break;
//
//            // your code for button2 here
//
//            // even more buttons here
//        }
//    }
//
////    @Override
////    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
////        Class clicked = CLASSES[position];
////        startActivity(new Intent(this, clicked));
////    }
//
//    public static class MyArrayAdapter extends ArrayAdapter<Class> {
//
//        private Context mContext;
//        private Class[] mClasses;
//        private int[] mDescriptionIds;
//
//        public MyArrayAdapter(Context context, int resource, Class[] objects) {
//            super(context, resource, objects);
//
//            mContext = context;
//            mClasses = objects;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View view = convertView;
//
//            if (convertView == null) {
//                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
//                view = inflater.inflate(android.R.layout.simple_list_item_2, null);
//            }
//
//            ((TextView) view.findViewById(android.R.id.text1)).setText(mClasses[position].getSimpleName());
//            ((TextView) view.findViewById(android.R.id.text2)).setText(mDescriptionIds[position]);
//
//            return view;
//        }
//
//        public void setDescriptionIds(int[] descriptionIds) {
//            mDescriptionIds = descriptionIds;
//        }
//    }
//}
