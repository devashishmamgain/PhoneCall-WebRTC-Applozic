package com.applozic.mobicomkit.uiwidgets.people.activity;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;

import com.applozic.mobicomkit.uiwidgets.conversation.activity.SearchActivityInterface;
import com.applozic.mobicomkit.uiwidgets.people.channel.ChannelFragment;
import com.applozic.mobicomkit.uiwidgets.people.contact.AppContactFragment;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.OnContactsInteractionListener;
import com.applozic.mobicommons.people.SearchListFragment;
import com.applozic.mobicommons.people.contact.Contact;
import com.applozic.mobicommons.people.contact.ContactUtils;

import com.applozic.mobicommons.people.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MobiComKitPeopleActivity extends AppCompatActivity implements OnContactsInteractionListener,
        SearchView.OnQueryTextListener,SearchActivityInterface,TabLayout.OnTabSelectedListener {

    public static final String SHARED_TEXT = "SHARED_TEXT";
    public static final String FORWARD_MESSAGE = "forwardMessage";
    private static final String CONTACT_ID = "contactId";
    public static final String GROUP_ID = "groupId";
    private static final String GROUP_NAME = "groupName";
    public static final String USER_ID = "userId";
    public static final String USER_ID_ARRAY = "userIdArray";
    public static final String GROUP_TAB = "GROUP_TAB";
    protected SearchView searchView;
    protected String searchTerm;
    private SearchListFragment searchListFragment;
    private boolean isSearchResultView = false;
    ApplozicSetting applozicSetting;
    ViewPager viewPager;
    TabLayout tabLayout;
    ActionBar actionBar;
    ViewPagerAdapter adapter;
    String[] userIdArray;
    public LinearLayout layout;
    boolean openGroupTab;

    AppContactFragment appContactFragment;
    ChannelFragment channelFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.people_activity);
        applozicSetting = ApplozicSetting.getInstance(getBaseContext());

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        layout = (LinearLayout) findViewById(R.id.footerAd);

        // Set up the action bar.
        actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        if (getIntent().getExtras() != null) {
            userIdArray = getIntent().getStringArrayExtra(USER_ID_ARRAY);
            openGroupTab = getIntent().getBooleanExtra(GROUP_TAB,false);
        }
        appContactFragment =  new AppContactFragment(userIdArray);
        channelFragment = new ChannelFragment();
        setSearchListFragment(appContactFragment);
        if(applozicSetting.isStartNewGroupButtonVisible()){
            actionBar.setTitle(getString(R.string.search_title));
            viewPager = (ViewPager) findViewById(R.id.viewPager);
            viewPager.setVisibility(View.VISIBLE);
            setupViewPager(viewPager);
            if(openGroupTab){
                viewPager.setCurrentItem(1);
            }
            tabLayout = (TabLayout) findViewById(R.id.tab_layout);
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            tabLayout.setOnTabSelectedListener(this);
        }else {
            actionBar.setTitle(getString(R.string.search_title));
            //Todo: check if this can be replaced with appContactFragment
            addFragment(this,new AppContactFragment(userIdArray),"AppContactFragment");
        }
      /*  mContactsListFragment = (AppContactFragment)
                getSupportFragmentManager().findFragmentById(R.id.contact_list);*/

        // This flag notes that the Activity is doing a search, and so the result will be
        // search results rather than all contacts. This prevents the Activity and Fragment
        // from trying to a search on search results.
        isSearchResultView = true;

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();

        String searchQuery = intent.getStringExtra(SearchManager.QUERY);
        // Set special title for search results
        String title = getString(R.string.contacts_list_search_results_title, searchQuery);
        setTitle(title);

      /*  if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mContactsListFragment.onQueryTextChange(searchQuery);
        }*/
        if (!MobiComUserPreference.getInstance(this).isDeviceContactSyncCompleted()) {
            showSnackBar(R.string.applozic_device_contact_sync_in_progress);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        if(Utils.hasICS()){
            searchItem.collapseActionView();
        }
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        searchView.setIconified(true);
        return super.onCreateOptionsMenu(menu);
    }

    public static void addFragment(FragmentActivity fragmentActivity, Fragment fragmentToAdd, String fragmentTag) {
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, fragmentToAdd,
                fragmentTag);

        if (supportFragmentManager.getBackStackEntryCount() > 1) {
            supportFragmentManager.popBackStack();
        }
        fragmentTransaction.addToBackStack(fragmentTag);
        fragmentTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
    }
    /**
     * This interface callback lets the main contacts list fragment notify
     * this activity that a contact has been selected.
     *
     * @param contactUri The contact Uri to the selected contact.
     */
    @Override
    public void onContactSelected(Uri contactUri) {
        Long contactId = ContactUtils.getContactId(getContentResolver(), contactUri);
        Map<String, String> phoneNumbers = ContactUtils.getPhoneNumbers(getApplicationContext(), contactId);

        if (phoneNumbers.isEmpty()) {
            Toast toast = Toast.makeText(this.getApplicationContext(), R.string.phone_number_not_present, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(CONTACT_ID, contactId);
        intent.setData(contactUri);
        finishActivity(intent);
    }

    public void startNewConversation(String contactNumber) {
        Intent intent = new Intent();
        intent.putExtra(USER_ID, contactNumber);
        finishActivity(intent);
    }

    @Override
    public void onGroupSelected(Channel channel) {
        Intent intent = new Intent();
        intent.putExtra(GROUP_ID, channel.getKey());
        intent.putExtra(GROUP_NAME, channel.getName());
        setResult(RESULT_OK, intent);
        finishActivity(intent);
    }

    @Override
    public void onCustomContactSelected(Contact contact) {
        Intent intent = new Intent();
        intent.putExtra(USER_ID, contact.getUserId());
        setResult(RESULT_OK, intent);
        finishActivity(intent);
    }

    public void finishActivity(Intent intent) {
        String forwardMessage = getIntent().getStringExtra(FORWARD_MESSAGE);
        if (!TextUtils.isEmpty(forwardMessage)) {
            intent.putExtra(FORWARD_MESSAGE, forwardMessage);
        }

        String sharedText = getIntent().getStringExtra(SHARED_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            intent.putExtra(SHARED_TEXT, sharedText);
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSelectionCleared() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
            // For platforms earlier than Android 3.0, triggers the search activity
        } else if (i == R.id.menu_search) {// if (!Utils.hasHoneycomb()) {
            onSearchRequested();
            //}

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        // Don't allow another search if this activity instance is already showing
        // search results. Only used pre-HC.
        return !isSearchResultView && super.onSearchRequested();
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if(applozicSetting.isCreateAnyContact()){
            this.searchTerm = query;
            startNewConversation(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        this.searchTerm = query;
        if(getSearchListFragment()!=null){
            getSearchListFragment().onQueryTextChange(query);
        }
        return true;
    }

    public SearchListFragment getSearchListFragment() {
        return searchListFragment;
    }

    public void setSearchListFragment(SearchListFragment searchListFragment) {
        this.searchListFragment = searchListFragment;
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter  = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(appContactFragment, "Contact");
        adapter.addFrag(channelFragment, "Group");
        viewPager.setAdapter(adapter);
    }

    public void showSnackBar(int resId) {
        Snackbar snackbar = Snackbar.make(layout, resId,
                Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public void setSearchInterface(SearchListFragment searchFragment) {
        setSearchListFragment(searchFragment);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition(), true);
        switch (tab.getPosition()) {
            case 0:
                setSearchListFragment((AppContactFragment)adapter.getItem(0));
                if(getSearchListFragment() != null){
                    getSearchListFragment().onQueryTextChange(null);
                }
                break;
            case 1:
                setSearchListFragment((ChannelFragment)adapter.getItem(1));
                if(getSearchListFragment() != null){
                    getSearchListFragment().onQueryTextChange(null);
                }
                break;
        }

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition(),true);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> titleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            fragmentList.add(fragment);
            titleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList.get(position);
        }

    }

}
