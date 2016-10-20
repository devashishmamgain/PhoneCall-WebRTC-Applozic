package com.applozic.mobicomkit.uiwidgets.people.contact;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.Button;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.UserBlockTask;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.contact.database.ContactDatabase;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;
import com.applozic.mobicomkit.uiwidgets.alphanumbericcolor.AlphaNumberColorUtil;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.people.OnContactsInteractionListener;
import com.applozic.mobicommons.people.SearchListFragment;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by adarsh on 1/8/15.
 */

/**
 * Created by adarsh on 30/5/15.
 */
@SuppressLint("ValidFragment")
public class AppContactFragment extends ListFragment implements SearchListFragment,
        AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PACKAGE_TO_EXCLUDE_FOR_INVITE = "net.mobitexter";
    // Defines a tag for identifying log entries
    private static final String TAG = "AppContactFragment";
    private static final String SHARE_TEXT = "share_text";
    // Bundle key for saving previously selected search result item
    private static final String STATE_PREVIOUSLY_SELECTED_KEY =
            "net.mobitexter.mobiframework.contact.ui.SELECTED_ITEM";
    private static String inviteMessage;
    private ContactsAdapter mAdapter; // The main query adapter
    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread
    private String mSearchTerm; // Stores the current search query term

    // Contact selected listener that allows the activity holding this fragment to be notified of
// a contact being selected
    private OnContactsInteractionListener mOnContactSelectedListener;

    // Stores the previously selected search item so that on a configuration change the same item
// can be reselected again
    private int mPreviouslySelectedSearchItem = 0;
    private BaseContactService contactService;

    Activity activity;

    private Button shareButton;
    private TextView resultTextView;

    private List<Contact> contactList;
    private boolean syncStatus = true;
    private String[] userIdArray;
    MobiComUserPreference mobiComUserPreference;
    ContactDatabase contactDatabase;
    String countryCode;
    //private RelativeLayout shareContactRelativeLayout;

    static int CONSTANT_TIME = 60 * 1000;

    /**
     * Fragments require an empty constructor.
     */
    public AppContactFragment() {

    }

    public AppContactFragment(String[] userIdArray) {
        this.userIdArray = userIdArray;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mobiComUserPreference = MobiComUserPreference.getInstance(getActivity());
        contactService = new AppContactService(getActivity());
        contactDatabase = new ContactDatabase(getContext());
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        countryCode = telephonyManager.getSimCountryIso().toUpperCase();
        mAdapter = new ContactsAdapter(getActivity().getApplicationContext());
        inviteMessage = Utils.getMetaDataValue(getActivity().getApplicationContext(), SHARE_TEXT);
        if (savedInstanceState != null) {

            mSearchTerm = savedInstanceState.getString(SearchManager.QUERY);
            mPreviouslySelectedSearchItem =
                    savedInstanceState.getInt(STATE_PREVIOUSLY_SELECTED_KEY, 0);
        }
        final Context context = getActivity();
        mImageLoader = new ImageLoader(context, getListPreferredItemHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return contactService.downloadContactImage(context, (Contact) data);
            }
        };
        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.applozic_ic_contact_picture_holo_light);
        // add a cache to the image loader
        mImageLoader.addImageCache(getActivity().getSupportFragmentManager(), 0.1f);
        mImageLoader.setImageFadeIn(false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the list fragment layout
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);
        shareButton = (Button) view.findViewById(R.id.actionButton);
        shareButton.setVisibility(ApplozicSetting.getInstance(getActivity()).isInviteFriendsButtonVisible() ? View.VISIBLE : View.GONE);
        resultTextView = (TextView) view.findViewById(R.id.result);
        /*shareContactRelativeLayout =  (RelativeLayout)view.findViewById(R.id.contact_invite_whatapp_contacts);

        shareContactRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Share contact logic should go here.
                Log.i("Test",  "OnClick listener called");
            }
        });*/


        return view;
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        shareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openInvite();
            }
        });

        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause image loader to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageLoader.setPauseWork(true);
                    Utils.toggleSoftKeyBoard(getActivity(), true);
                } else {
                    mImageLoader.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });

        // If there's a previously selected search item from a saved state then don't bother
        // initializing the loader as it will be restarted later when the query is populated into
        // the action bar search view (see onQueryTextChange() in onCreateOptionsMenu()).
        if (mPreviouslySelectedSearchItem == 0) {
            // Initialize the loader, and create a loader identified by ContactsQuery.QUERY_ID
            getLoaderManager().initLoader(ContactsQuery.QUERY_ID, null, this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ((MobiComKitPeopleActivity)getActivity()).setSearchListFragment(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;

        try {
            // Assign callback listener which the holding activity must implement. This is used
            // so that when a contact item is interacted with (selected by the user) the holding
            // activity will be notified and can take further action such as populating the contact
            // detail pane (if in multi-pane layout) or starting a new activity with the contact
            // details (single pane layout).
            mOnContactSelectedListener = (OnContactsInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnContactsInteractionListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // In the case onPause() is called during a fling the image loader is
        // un-paused to let any remaining background work complete.
        mImageLoader.setPauseWork(false);
    }


    public void openInvite() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND)
                .setType("text/plain").putExtra(Intent.EXTRA_TEXT, inviteMessage);

        List<Intent> targetedShareIntents = new ArrayList<Intent>();

        List<ResolveInfo> resInfo = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                if (packageName.equals(PACKAGE_TO_EXCLUDE_FOR_INVITE)) {
                    continue;
                }
                Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                targetedShareIntent.setType("text/plain")
                        .setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, inviteMessage)
                        .setPackage(packageName);
                targetedShareIntents.add(targetedShareIntent);
            }
            Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), "Share Via");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
            startActivity(chooserIntent);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

        final Cursor cursor = mAdapter.getCursor();

        // Moves to the Cursor row corresponding to the ListView item that was clicked
        cursor.moveToPosition(position);
        Contact contact = contactDatabase.getContact(cursor, "_id");

        if (contact.isBlocked()) {
            userUnBlockDialog(contact);
            return;
        }

        //TODO: place Invite code here.Invite view is invisible, make visibility here based on condition.
        if (contact.isDeviceContact()) {
            Log.i(TAG, "Device contact invite operation should be called");
            openInvite();
            return;
        }

        mOnContactSelectedListener.onCustomContactSelected(contact);
    }

    /**
     * Called when ListView selection is cleared, for example
     * when search mode is finished and the currently selected
     * contact should no longer be selected.
     */
    @SuppressLint("NewApi")
    private void onSelectionCleared() {
        // Uses callback to notify activity this contains this fragment
        mOnContactSelectedListener.onSelectionCleared();
        // Clears currently checked item
        getListView().clearChoices();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(mSearchTerm)) {
            // Saves the current search string
            outState.putString(SearchManager.QUERY, mSearchTerm);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Updates
        // the search filter, and restarts the loader to do a new query
        // using the new search string.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

        // Don't do anything if the filter is empty

        // Updates current filter to new filter
        mSearchTerm = newFilter;
        mAdapter.indexOfSearchQuery(newFilter);

        getLoaderManager().restartLoader(
                AppContactFragment.ContactsQuery.QUERY_ID, null, AppContactFragment.this);

        return true;
    }

    /**
     * Gets the preferred height for each item in the ListView, in pixels, after accounting for
     * screen density. ImageLoader uses this value to resize thumbnail images to match the ListView
     * item height.
     *
     * @return The preferred height in pixels, based on the current theme.
     */
    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();

        // Resolve list item preferred height theme attribute into typedValue
        getActivity().getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);

// Create a new DisplayMetrics object
        final DisplayMetrics metrics = new DisplayMetrics();

        // Populate the DisplayMetrics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Loader<Cursor> loader = contactDatabase.getSearchCursorLoader(mSearchTerm, ApplozicSetting.getInstance(getActivity()).getTotalOnlineUser(), userIdArray);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // This swaps the new cursor into the adapter.
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == ContactsQuery.QUERY_ID) {
            // When the loader is being reset, clear the cursor from the adapter. This allows the
            // cursor resources to be freed.
            mAdapter.swapCursor(null);
        }
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     */
    public interface ContactsQuery {
        // An identifier for the loader
        int QUERY_ID = 1;

    }


    /**
     * This is a subclass of CursorAdapter that supports binding Cursor columns to a view layout.
     * If those items are part of search results, the search string is marked by highlighting the
     * query text. An {@link android.widget.AlphabetIndexer} is used to allow quicker navigation up and down the
     * ListView.
     */
    private class ContactsAdapter extends CursorAdapter implements SectionIndexer {
        Context context;
        private LayoutInflater mInflater; // Stores the layout inflater
        private AlphabetIndexer mAlphabetIndexer; // Stores the AlphabetIndexer instance
        private TextAppearanceSpan highlightTextSpan; // Stores the highlight text appearance style

        /**
         * Instantiates a new Contacts Adapter.
         *
         * @param context A context that has access to the app's layout.
         */
        public ContactsAdapter(Context context) {
            super(context, null, 0);
            this.context = context;
            // Stores inflater for use later
            mInflater = LayoutInflater.from(context);
            // Loads a string containing the English alphabet. To fully localize the app, provide a
            // strings.xml file in res/values-<x> directories, where <x> is a locale. In the file,
            // define a string with android:name="alphabet" and contents set to all of the
            // alphabetic characters in the language in their proper sort order, in upper case if
            // applicable.
            final String alphabet = context.getString(R.string.alphabet);

            // Instantiates a new AlphabetIndexer bound to the column used to sort contact names.
            // The cursor is left null, because it has not yet been retrieved.
            mAlphabetIndexer = new AlphabetIndexer(null, 1, alphabet);

            // Defines a span for highlighting the part of a display name that matches the search
            // string
            highlightTextSpan = new TextAppearanceSpan(getActivity(), R.style.searchTextHiglight);
        }

        /**
         * Identifies the start of the search string in the display name column of a Cursor row.
         * E.g. If displayName was "Adam" and search query (mSearchTerm) was "da" this would
         * return 1.
         *
         * @param displayName The contact display name.
         * @return The starting position of the search string in the display name, 0-based. The
         * method returns -1 if the string is not found in the display name, or if the search
         * string is empty or null.
         */
        private int indexOfSearchQuery(String displayName) {
            if (!TextUtils.isEmpty(mSearchTerm)) {
                return displayName.toLowerCase(Locale.getDefault()).indexOf(
                        mSearchTerm.toLowerCase(Locale.getDefault()));
            }
            return -1;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View itemLayout =
                    mInflater.inflate(R.layout.contact_list_item, parent, false);

            final ViewHolder holder = new ViewHolder();
            holder.text1 = (TextView) itemLayout.findViewById(R.id.text1);
            holder.text2 = (TextView) itemLayout.findViewById(R.id.text2);
            holder.icon = (CircleImageView) itemLayout.findViewById(R.id.contactImage);
            holder.onlineTextView = (TextView) itemLayout.findViewById(R.id.onlineTextView);
            holder.alphabeticImage = (TextView) itemLayout.findViewById(R.id.alphabeticImage);
            holder.invite = (TextView) itemLayout.findViewById(R.id.invite);
            holder.unBlock = (TextView) itemLayout.findViewById(R.id.unblock);
            holder.mobile = (TextView) itemLayout.findViewById(R.id.mobile);
            itemLayout.setTag(holder);
            return itemLayout;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            // Gets handles to individual view resources
            final ViewHolder holder = (ViewHolder) view.getTag();

            //////////////////////////
            Contact contact = contactDatabase.getContact(cursor, "_id");
            String contactNumber;
            char firstLetter;
            ///////////////////

            holder.text1.setText(contact.getDisplayName() == null ? contact.getUserId() : contact.getDisplayName());
            holder.text2.setVisibility(View.GONE);
            holder.onlineTextView.setVisibility(contact != null && contact.isOnline() ? View.VISIBLE : View.GONE);
            holder.unBlock.setVisibility(View.GONE);
            holder.invite.setVisibility(View.GONE);
            holder.mobile.setVisibility(View.GONE);

            if (contact.isDeviceContact()) {
                holder.invite.setVisibility(View.VISIBLE);
            } else if (contact.isBlocked()) {
                holder.unBlock.setVisibility(View.VISIBLE);
            }

            if (contact.isDeviceContactAndRegistered()) {
                if (!contact.isBlocked()) {
                    holder.mobile.setVisibility(View.VISIBLE);
                }
            }

            if (contact.getContactType() == 2) {
                StringBuffer stringBuffer = ChannelService.getInstance(context).getChannelNamesByUserId(contact.getUserId());
                if (stringBuffer != null && stringBuffer.length() > 0) {
                    int lastIndex = stringBuffer.lastIndexOf(",");
                    String groupNames = stringBuffer.replace(lastIndex, lastIndex + 1, "").toString();
                    holder.text2.setText(groupNames);
                    holder.text2.setVisibility(View.VISIBLE);
                } else {
                    holder.text2.setText(contact.getFormattedContactNumber());
                    holder.text2.setVisibility(View.VISIBLE);
                }
            } else {
                holder.text2.setText(contact.getFormattedContactNumber());
                holder.text2.setVisibility(View.VISIBLE);
            }


            // Returns the item layout view

            if (contact != null && !TextUtils.isEmpty(contact.getDisplayName())) {
                contactNumber = contact.getDisplayName().toUpperCase();
                firstLetter = contact.getDisplayName().toUpperCase().charAt(0);
                if (firstLetter != '+') {
                    holder.alphabeticImage.setText(String.valueOf(firstLetter));
                } else if (contactNumber.length() >= 2) {
                    holder.alphabeticImage.setText(String.valueOf(contactNumber.charAt(1)));
                }
                Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
                GradientDrawable bgShape = (GradientDrawable) holder.alphabeticImage.getBackground();
                bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));
            }
            holder.alphabeticImage.setVisibility(View.GONE);
            holder.icon.setVisibility(View.VISIBLE);
            if (contact != null) {
                if (contact.isDrawableResources()) {
                    int drawableResourceId = context.getResources().getIdentifier(contact.getrDrawableName(), "drawable", context.getPackageName());
                    holder.icon.setImageResource(drawableResourceId);
                } else {
                    mImageLoader.loadImage(contact, holder.icon, holder.alphabeticImage);
                }
            }

            ///////////////////////
            final int startIndex = indexOfSearchQuery(contact.getDisplayName());

            if (startIndex == -1) {
                // If the user didn't do a search, or the search string didn't match a display
                // name, show the display name without highlighting
                holder.text1.setText(contact.getDisplayName());
                // holder.text2.setVisibility(View.VISIBLE);
            } else {
                // If the search string matched the display name, applies a SpannableString to
                // highlight the search string with the displayed display name

                // Wraps the display name in the SpannableString
                final SpannableString highlightedName = new SpannableString(contact.getDisplayName());

                // Sets the span to start at the starting point of the match and end at "length"
                // characters beyond the starting point
                highlightedName.setSpan(highlightTextSpan, startIndex,
                        startIndex + mSearchTerm.length(), 0);

                // Binds the SpannableString to the display name View object
                holder.text1.setText(highlightedName);

            }

        }

        /**
         * Overrides swapCursor to move the new Cursor into the AlphabetIndex as well as the
         * CursorAdapter.
         */
        @Override
        public Cursor swapCursor(Cursor newCursor) {
            // Update the AlphabetIndexer with new cursor as well
            mAlphabetIndexer.setCursor(newCursor);
            return super.swapCursor(newCursor);
        }

        /**
         * An override of getCount that simplifies accessing the Cursor. If the Cursor is null,
         * getCount returns zero. As a result, no test for Cursor == null is needed.
         */
        @Override
        public int getCount() {
            if (getCursor() == null) {
                return 0;
            }
            return super.getCount();
        }

        /**
         * Defines the SectionIndexer.getSections() interface.
         */
        @Override
        public Object[] getSections() {
            return mAlphabetIndexer.getSections();
        }

        /**
         * Defines the SectionIndexer.getPositionForSection() interface.
         */
        @Override
        public int getPositionForSection(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getPositionForSection(i);
        }

        /**
         * Defines the SectionIndexer.getSectionForPosition() interface.
         */
        @Override
        public int getSectionForPosition(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getSectionForPosition(i);
        }

        /**
         * A class that defines fields for each resource ID in the list item layout. This allows
         * ContactsAdapter.newView() to store the IDs once, when it inflates the layout, instead of
         * calling findViewById in each iteration of bindView.
         */
        private class ViewHolder {
            TextView text1;
            TextView text2;
            CircleImageView icon;
            TextView onlineTextView, alphabeticImage;
            TextView invite;
            TextView unBlock;
            TextView mobile;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(
                AppContactFragment.ContactsQuery.QUERY_ID, null, AppContactFragment.this);

       if (mobiComUserPreference.getDeviceContactSyncTime() != 0) {
            Date date = new Date();
            if ((date.getTime() - mobiComUserPreference.getDeviceContactSyncTime()) >= CONSTANT_TIME) {
                Intent intent = new Intent(getActivity(), DeviceContactSyncService.class);
                intent.putExtra(DeviceContactSyncService.PROCESS_USER_DETAILS, true);
                getActivity().startService(intent);
            } else if (mobiComUserPreference.isSyncRequired()) {
                MobiComUserPreference.getInstance(getActivity()).setSyncContacts(false);
                Intent intent = new Intent(getActivity(), DeviceContactSyncService.class);
                intent.putExtra(DeviceContactSyncService.PROCESS_MODIFIED_DEVICE_CONTACTS, true);
                getActivity().startService(intent);
            }
        }
    }

    public void blockUserProcess(final Contact contact, final boolean block) {
        final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "",
                getActivity().getString(R.string.please_wait_info), true);

        UserBlockTask.TaskListener listener = new UserBlockTask.TaskListener() {

            @Override
            public void onSuccess(ApiResponse apiResponse) {
                getLoaderManager().restartLoader(
                        AppContactFragment.ContactsQuery.QUERY_ID, null, AppContactFragment.this);
            }

            @Override
            public void onFailure(ApiResponse apiResponse, Exception exception) {
                String error = getString(Utils.isInternetAvailable(getActivity()) ? R.string.applozic_server_error : R.string.you_need_network_access_for_block_or_unblock);
                Toast toast = Toast.makeText(getActivity(), error, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            @Override
            public void onCompletion() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

        };

        new UserBlockTask(getActivity(), listener, contact.getUserId(), block).execute((Void) null);
    }

    public void userUnBlockDialog(final Contact contact) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity()).
                setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        blockUserProcess(contact, false);
                    }
                });
        alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        String name = contact.getDisplayName();
        alertDialog.setMessage(getString(R.string.user_un_block_info).replace("[name]", name));
        alertDialog.setCancelable(true);
        alertDialog.create().show();
    }

}
