package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EditorProfileListFragment extends Fragment
                                        implements OnStartDragItemListener {

    public DataWrapper activityDataWrapper;

    RecyclerView listView;
    private TextView activeProfileName;
    private ImageView activeProfileIcon;
    private Toolbar bottomToolbar;
    TextView textViewNoData;
    private LinearLayout progressBar;

    private EditorProfileListAdapter profileListAdapter;
    private ItemTouchHelper itemTouchHelper;

    private WeakReference<LoadProfileListAsyncTask> asyncTaskContext;

    public static final int EDIT_MODE_UNDEFINED = 0;
    public static final int EDIT_MODE_INSERT = 1;
    public static final int EDIT_MODE_DUPLICATE = 2;
    public static final int EDIT_MODE_EDIT = 3;
    public static final int EDIT_MODE_DELETE = 4;

    public static final String FILTER_TYPE_ARGUMENT = "filter_type";
    public static final String START_TARGET_HELPS_ARGUMENT = "start_target_helps";

    public static final int FILTER_TYPE_ALL = 0;
    public static final int FILTER_TYPE_SHOW_IN_ACTIVATOR = 1;
    public static final int FILTER_TYPE_NO_SHOW_IN_ACTIVATOR = 2;

    public boolean targetHelpsSequenceStarted;
    public static final String PREF_START_TARGET_HELPS = "editor_profile_list_fragment_start_target_helps";

    private int filterType = FILTER_TYPE_ALL;

    /**
     * The fragment's current callback objects
     */
    private OnStartProfilePreferences onStartProfilePreferencesCallback = sDummyOnStartProfilePreferencesCallback;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified.
     */
    // invoked when start profile preference fragment/activity needed
    interface OnStartProfilePreferences {
        void onStartProfilePreferences(Profile profile, int editMode, int predefinedProfileIndex/*, boolean startTargetHelps*/);
    }

    /**
     * A dummy implementation of the Callbacks interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final OnStartProfilePreferences sDummyOnStartProfilePreferencesCallback = new OnStartProfilePreferences() {
        public void onStartProfilePreferences(Profile profile, int editMode, int predefinedProfileIndex/*, boolean startTargetHelps*/) {
        }
    };

    public EditorProfileListFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof OnStartProfilePreferences)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        onStartProfilePreferencesCallback = (OnStartProfilePreferences) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        onStartProfilePreferencesCallback = sDummyOnStartProfilePreferencesCallback;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // this is really important in order to save the state across screen
        // configuration changes for example
        setRetainInstance(true);

        filterType = getArguments() != null ? 
                getArguments().getInt(FILTER_TYPE_ARGUMENT, EditorProfileListFragment.FILTER_TYPE_ALL) :
                    EditorProfileListFragment.FILTER_TYPE_ALL;

        activityDataWrapper = new DataWrapper(getActivity().getApplicationContext(), false, 0);

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;

        if (ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context) && ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            rootView = inflater.inflate(R.layout.editor_profile_list, container, false);
        else
        if (ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            rootView = inflater.inflate(R.layout.editor_profile_list_no_indicator, container, false);
        else
            rootView = inflater.inflate(R.layout.editor_profile_list_no_header, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        doOnViewCreated(view/*, savedInstanceState*/);

        boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
        if (startTargetHelps)
            showTargetHelps();
    }

    @SuppressLint("InflateParams")
    private void doOnViewCreated(View view/*, Bundle savedInstanceState*/)
    {
        //super.onActivityCreated(savedInstanceState);

    /*	activeProfileName = getActivity().findViewById(R.id.activated_profile_name);
        activeProfileIcon = getActivity().findViewById(R.id.activated_profile_icon);
        listView = getActivity().findViewById(R.id.editor_profiles_list);
        listView.setEmptyView(getActivity().findViewById(R.id.editor_profiles_list_empty));
    */
        activeProfileName = view.findViewById(R.id.activated_profile_name);
        activeProfileIcon = view.findViewById(R.id.activated_profile_icon);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        listView = view.findViewById(R.id.editor_profiles_list);
        listView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        listView.setLayoutManager(layoutManager);
        listView.setHasFixedSize(true);
        textViewNoData = view.findViewById(R.id.editor_profiles_list_empty);
        progressBar = view.findViewById(R.id.editor_profiles_list_linla_progress);

        final Activity activity = getActivity();
        final EditorProfileListFragment fragment = this;

        bottomToolbar = getActivity().findViewById(R.id.editor_list_bottom_bar);
        Menu menu = bottomToolbar.getMenu();
        if (menu != null) menu.clear();
        bottomToolbar.inflateMenu(R.menu.editor_profiles_bottom_bar);
        bottomToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_add_profile:
                        if (profileListAdapter != null) {
                            ((EditorProfilesActivity) getActivity()).addProfileDialog = new AddProfileDialog(activity, fragment);
                            ((EditorProfilesActivity) getActivity()).addProfileDialog.show();
                        }
                        return true;
                    case R.id.menu_delete_all_profiles:
                        deleteAllProfiles();
                        return true;
                    case R.id.menu_default_profile:
                        Intent intent = new Intent(getActivity(), PhoneProfilesPreferencesActivity.class);
                        intent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "profileActivationCategory");
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });

        LinearLayout orderLayout = getActivity().findViewById(R.id.editor_list_bottom_bar_order_root);
        orderLayout.setVisibility(View.GONE);

        synchronized (activityDataWrapper.profileList) {
            if (!activityDataWrapper.profileListFilled) {
                LoadProfileListAsyncTask asyncTask = new LoadProfileListAsyncTask(this, filterType);
                this.asyncTaskContext = new WeakReference<>(asyncTask);
                asyncTask.execute();
            } else {
                listView.setAdapter(profileListAdapter);

                // update activity for activated profile
                fragment.listView.getRecycledViewPool().clear();
                Profile profile = activityDataWrapper.getActivatedProfile(true,
                        ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context));
                updateHeader(profile);
                profileListAdapter.notifyDataSetChanged(false);
                if (!ApplicationPreferences.applicationEditorHeader(fragment.activityDataWrapper.context))
                    setProfileSelection(profile);
            }
        }

    }

    private static class LoadProfileListAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<EditorProfileListFragment> fragmentWeakRef;
        private final DataWrapper _dataWrapper;
        //private final Context _baseContext;
        private final int _filterType;
        boolean defaultProfilesGenerated = false;
        boolean defaultEventsGenerated = false;

        private LoadProfileListAsyncTask (EditorProfileListFragment fragment, int filterType) {
            fragmentWeakRef = new WeakReference<>(fragment);
            _filterType = filterType;
            _dataWrapper = new DataWrapper(fragment.getActivity().getApplicationContext(), false, 0);
            //_baseContext = fragment.getActivity();
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            EditorProfileListFragment fragment = this.fragmentWeakRef.get();

            if ((fragment != null) && (fragment.isAdded())) {
                fragment.textViewNoData.setVisibility(View.GONE);
                fragment.progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            _dataWrapper.fillProfileList(true, ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
            if (_dataWrapper.profileList.size() == 0)
            {
                // no profiles in DB, generate default profiles and events
                EditorProfileListFragment fragment = this.fragmentWeakRef.get();
                if ((fragment != null) && (fragment.getActivity() != null)) {
                    _dataWrapper.fillPredefinedProfileList(true, ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context), fragment.getActivity());
                    defaultProfilesGenerated = true;
                }
                if ((fragment != null) && (fragment.getActivity() != null)) {
                    _dataWrapper.generatePredefinedEventList(fragment.getActivity());
                    defaultEventsGenerated = true;
                }
            }
            // sort list
            if (_filterType != EditorProfileListFragment.FILTER_TYPE_SHOW_IN_ACTIVATOR)
                EditorProfileListFragment.sortAlphabetically(_dataWrapper.profileList);
            else
                EditorProfileListFragment.sortByPOrder(_dataWrapper.profileList);

            return null;
        }

        @Override
        protected void onPostExecute(Void response) {
            super.onPostExecute(response);
            
            EditorProfileListFragment fragment = fragmentWeakRef.get();
            
            if ((fragment != null) && (fragment.isAdded())) {
                fragment.progressBar.setVisibility(View.GONE);

                // get local profileList
                _dataWrapper.fillProfileList(true, ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
                // set local profile list into activity dataWrapper
                fragment.activityDataWrapper.copyProfileList(_dataWrapper);

                fragment.profileListAdapter = new EditorProfileListAdapter(fragment, fragment.activityDataWrapper, _filterType, fragment);

                // added touch helper for drag and drop items
                ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(fragment.profileListAdapter, false, false);
                fragment.itemTouchHelper = new ItemTouchHelper(callback);
                fragment.itemTouchHelper.attachToRecyclerView(fragment.listView);

                fragment.listView.setAdapter(fragment.profileListAdapter);

                // update activity for activated profile
                fragment.listView.getRecycledViewPool().clear();
                Profile profile = fragment.activityDataWrapper.getActivatedProfile(true,
                                ApplicationPreferences.applicationEditorPrefIndicator(_dataWrapper.context));
                fragment.updateHeader(profile);
                fragment.profileListAdapter.notifyDataSetChanged(false);
                if (!ApplicationPreferences.applicationEditorHeader(_dataWrapper.context))
                    fragment.setProfileSelection(profile);

                if (defaultProfilesGenerated)
                {
                    ActivateProfileHelper.updateGUI(_dataWrapper.context, true);
                    Toast msg = Toast.makeText(_dataWrapper.context,
                            fragment.getResources().getString(R.string.toast_default_profiles_generated),
                            Toast.LENGTH_SHORT);
                    msg.show();
                }
                if (defaultEventsGenerated)
                {
                    Toast msg = Toast.makeText(_dataWrapper.context,
                            fragment.getResources().getString(R.string.toast_default_events_generated),
                            Toast.LENGTH_SHORT);
                    msg.show();
                }
            }
        }
    }

    boolean isAsyncTaskPendingOrRunning() {
        try {
            return this.asyncTaskContext != null &&
                    this.asyncTaskContext.get() != null &&
                    !this.asyncTaskContext.get().getStatus().equals(AsyncTask.Status.FINISHED);
        } catch (Exception e) {
            return false;
        }
    }

    void stopRunningAsyncTask() {
        this.asyncTaskContext.get().cancel(true);
    }

    @Override
    public void onDestroy()
    {
        if (isAsyncTaskPendingOrRunning()) {
            stopRunningAsyncTask();
        }

        if (listView != null)
            listView.setAdapter(null);
        if (profileListAdapter != null)
            profileListAdapter.release();

        if (activityDataWrapper != null)
            activityDataWrapper.invalidateDataWrapper();
        activityDataWrapper = null;

        super.onDestroy();

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    public void startProfilePreferencesActivity(Profile profile, int predefinedProfileIndex)
    {
        int editMode;

        if (profile != null)
        {
            // edit profile
            int profilePos = profileListAdapter.getItemPosition(profile);
            /*int last = listView.getLastVisiblePosition();
            int first = listView.getFirstVisiblePosition();
            if ((profilePos <= first) || (profilePos >= last)) {
                listView.setSelection(profilePos);
            }*/
            listView.getLayoutManager().scrollToPosition(profilePos);

            boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
            if (startTargetHelps)
                showAdapterTargetHelps();

            editMode = EDIT_MODE_EDIT;
        }
        else
        {
            // add new profile
            editMode = EDIT_MODE_INSERT;
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) one must start profile preferences
        onStartProfilePreferencesCallback.onStartProfilePreferences(profile, editMode, predefinedProfileIndex);
    }

    private void duplicateProfile(Profile origProfile)
    {
        int editMode;

        // duplicate profile
        editMode = EDIT_MODE_DUPLICATE;

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) one must start profile preferences
        onStartProfilePreferencesCallback.onStartProfilePreferences(origProfile, editMode, 0);

    }

    private void deleteProfile(Profile profile)
    {
        //final Profile _profile = profile;
        //final Activity activity = getActivity();

        if (activityDataWrapper.getProfileById(profile._id, false, false, false) == null)
            // profile not exists
            return;

        activityDataWrapper.addActivityLog(DatabaseHandler.ALTYPE_PROFILEDELETED, null, profile._name, profile._icon, 0);

        Profile activatedProfile = activityDataWrapper.getActivatedProfile(false, false);
        if ((activatedProfile != null) && (activatedProfile._id == profile._id)) {
            // remove alarm for profile duration
            ProfileDurationAlarmBroadcastReceiver.removeAlarm(getActivity().getApplicationContext());
            Profile.setActivatedProfileForDuration(getActivity().getApplicationContext(), 0);
        }

        listView.getRecycledViewPool().clear();

        activityDataWrapper.stopEventsForProfileFromMainThread(profile, true);
        profileListAdapter.deleteItemNoNotify(profile);
        DatabaseHandler.getInstance(activityDataWrapper.context).deleteProfile(profile);

        profileListAdapter.notifyDataSetChanged();

        if (!Event.getGlobalEventsRunning(activityDataWrapper.context)) {
            //Profile profile = databaseHandler.getActivatedProfile();
            Profile _profile = profileListAdapter.getActivatedProfile();
            updateHeader(_profile);
            PPApplication.showProfileNotification(activityDataWrapper.context);
            ActivateProfileHelper.updateGUI(activityDataWrapper.context, true);
        }
        else
            activityDataWrapper.restartEvents(false, true/*, false*/, true, true);

        activityDataWrapper.setDynamicLauncherShortcutsFromMainThread();

        Intent serviceIntent = new Intent(getActivity().getApplicationContext(), PhoneProfilesService.class);
        serviceIntent.putExtra(PhoneProfilesService.EXTRA_REREGISTER_RECEIVERS_AND_JOBS, true);
        serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
        PPApplication.startPPService(getActivity(), serviceIntent);

        onStartProfilePreferencesCallback.onStartProfilePreferences(null, EDIT_MODE_DELETE, 0);
    }

    public void showEditMenu(View view)
    {
        //Context context = ((AppCompatActivity)getActivity()).getSupportActionBar().getThemedContext();
        Context context = view.getContext();
        PopupMenu popup;
        //if (android.os.Build.VERSION.SDK_INT >= 19)
            popup = new PopupMenu(context, view, Gravity.END);
        //else
        //    popup = new PopupMenu(context, view);
        getActivity().getMenuInflater().inflate(R.menu.profile_list_item_edit, popup.getMenu());

        final Profile profile = (Profile)view.getTag();

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            public boolean onMenuItemClick(android.view.MenuItem item) {
                switch (item.getItemId()) {
                case R.id.profile_list_item_menu_activate:
                    activateProfile(profile/*, true*/);
                    return true;
                case R.id.profile_list_item_menu_duplicate:
                    duplicateProfile(profile);
                    return true;
                case R.id.profile_list_item_menu_delete:
                    deleteProfileWithAlert(profile);
                    return true;
                default:
                    return false;
                }
            }
            });


        popup.show();
    }

    private void deleteProfileWithAlert(Profile profile)
    {
        final Profile _profile = profile;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getString(R.string.profile_string_0) + ": " + profile._name);
        dialogBuilder.setMessage(R.string.delete_profile_alert_message);
        //dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);

        dialogBuilder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteProfile(_profile);
            }
        });
        dialogBuilder.setNegativeButton(R.string.alert_button_no, null);
        AlertDialog dialog = dialogBuilder.create();
        /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                if (positive != null) positive.setAllCaps(false);
                Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                if (negative != null) negative.setAllCaps(false);
            }
        });*/
        dialog.show();
    }

    private void deleteAllProfiles()
    {
        if (profileListAdapter != null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(R.string.alert_title_delete_all_profiles);
            dialogBuilder.setMessage(R.string.alert_message_delete_all_profiles);
            //dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);

            //final Activity activity = getActivity();

            dialogBuilder.setPositiveButton(R.string.alert_button_yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    activityDataWrapper.addActivityLog(DatabaseHandler.ALTYPE_ALLPROFILESDELETED, null, null, null, 0);

                    // remove alarm for profile duration
                    ProfileDurationAlarmBroadcastReceiver.removeAlarm(getActivity().getApplicationContext());
                    Profile.setActivatedProfileForDuration(getActivity().getApplicationContext(), 0);

                    listView.getRecycledViewPool().clear();

                    activityDataWrapper.stopAllEventsFromMainThread(true, true);
                    profileListAdapter.clearNoNotify();
                    DatabaseHandler.getInstance(activityDataWrapper.context).deleteAllProfiles();
                    DatabaseHandler.getInstance(activityDataWrapper.context).unlinkAllEvents();

                    profileListAdapter.notifyDataSetChanged();

                    //Profile profile = databaseHandler.getActivatedProfile();
                    //Profile profile = profileListAdapter.getActivatedProfile();
                    updateHeader(null);
                    PPApplication.showProfileNotification(activityDataWrapper.context);
                    ActivateProfileHelper.updateGUI(activityDataWrapper.context, true);

                    activityDataWrapper.setDynamicLauncherShortcutsFromMainThread();

                    Intent serviceIntent = new Intent(getActivity().getApplicationContext(), PhoneProfilesService.class);
                    serviceIntent.putExtra(PhoneProfilesService.EXTRA_REREGISTER_RECEIVERS_AND_JOBS, true);
                    serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                    PPApplication.startPPService(getActivity(), serviceIntent);

                    onStartProfilePreferencesCallback.onStartProfilePreferences(null, EDIT_MODE_DELETE, 0);
                }
            });
            dialogBuilder.setNegativeButton(R.string.alert_button_no, null);
            AlertDialog dialog = dialogBuilder.create();
            /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button positive = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                    if (positive != null) positive.setAllCaps(false);
                    Button negative = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                    if (negative != null) negative.setAllCaps(false);
                }
            });*/
            dialog.show();
        }
    }

    public void updateHeader(Profile profile)
    {
        if (!ApplicationPreferences.applicationEditorHeader(activityDataWrapper.context))
            return;

        if ((activeProfileName == null) || (activeProfileIcon == null))
            return;

        if (profile == null)
        {
            activeProfileName.setText(getResources().getString(R.string.profiles_header_profile_name_no_activated));
            activeProfileIcon.setImageResource(R.drawable.ic_profile_default);
        }
        else
        {
            activeProfileName.setText(DataWrapper.getProfileNameWithManualIndicator(profile, true, true, false, activityDataWrapper, false));
            if (profile.getIsIconResourceID())
            {
                if (profile._iconBitmap != null)
                    activeProfileIcon.setImageBitmap(profile._iconBitmap);
                else {
                    //int res = getResources().getIdentifier(profile.getIconIdentifier(), "drawable", getActivity().getPackageName());
                    int res = Profile.getIconResource(profile.getIconIdentifier());
                    activeProfileIcon.setImageResource(res); // icon resource
                }
            }
            else
            {
                activeProfileIcon.setImageBitmap(profile._iconBitmap);
            }
        }

        if (ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context))
        {
            ImageView profilePrefIndicatorImageView = getActivity().findViewById(R.id.activated_profile_pref_indicator);
            if (profilePrefIndicatorImageView != null)
            {
                //profilePrefIndicatorImageView.setImageBitmap(ProfilePreferencesIndicator.paint(profile, getActivity().getBaseContext()));
                if (profile == null)
                    profilePrefIndicatorImageView.setImageResource(R.drawable.ic_empty);
                else {
                    if (profile._preferencesIndicator != null)
                        profilePrefIndicatorImageView.setImageBitmap(profile._preferencesIndicator);
                    else
                        profilePrefIndicatorImageView.setImageResource(R.drawable.ic_empty);
                }
            }
        }
    }

    public void doOnActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == EditorProfilesActivity.REQUEST_CODE_ACTIVATE_PROFILE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                long profile_id = data.getLongExtra(PPApplication.EXTRA_PROFILE_ID, -1);
                Profile profile = activityDataWrapper.getProfileById(profile_id, true,
                        ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context), false);

                if (profileListAdapter != null)
                    profileListAdapter.activateProfile(profile);
                updateHeader(profile);
             }
             //if (resultCode == Activity.RESULT_CANCELED)
             //{
                 //Write your code if there's no result
             //}
        }
    }

    public void activateProfile(Profile profile/*, boolean interactive*/)
    {
        activityDataWrapper.activateProfile(profile._id, PPApplication.STARTUP_SOURCE_EDITOR, getActivity()/*, ""*/);
    }

    private void setProfileSelection(Profile profile) {
        if (profileListAdapter != null)
        {
            int profilePos = ListView.INVALID_POSITION;

            if (profile != null)
                profilePos = profileListAdapter.getItemPosition(profile);
            //else
            //    profilePos = listView.getCheckedItemPosition();

            if (/*(!ApplicationPreferences.applicationEditorHeader(dataWrapper.context)) && */(profilePos != ListView.INVALID_POSITION))
            {
                // set profile visible in list
                //int last = listView.getLastVisiblePosition();
                //int first = listView.getFirstVisiblePosition();
                //if ((profilePos <= first) || (profilePos >= last)) {
                //    listView.setSelection(profilePos);
                //}
                listView.getLayoutManager().scrollToPosition(profilePos);
            }
        }

        boolean startTargetHelps = getArguments() != null && getArguments().getBoolean(START_TARGET_HELPS_ARGUMENT, false);
        if (startTargetHelps)
            showAdapterTargetHelps();
    }

    public void updateListView(Profile profile, boolean newProfile, boolean refreshIcons, boolean setPosition)
    {
        /*if (listView != null)
            listView.cancelDrag();*/

        if (profileListAdapter != null)
            listView.getRecycledViewPool().clear();

        if (profileListAdapter != null) {
            if ((newProfile) && (profile != null))
                // add profile into listview
                profileListAdapter.addItem(profile);
        }

        synchronized (activityDataWrapper.profileList) {
            if (!activityDataWrapper.profileListFilled) {
                // sort list
                if (filterType != EditorProfileListFragment.FILTER_TYPE_SHOW_IN_ACTIVATOR)
                    sortAlphabetically(activityDataWrapper.profileList);
                else
                    sortByPOrder(activityDataWrapper.profileList);
            }
        }

        if (profileListAdapter != null)
            profileListAdapter.notifyDataSetChanged(refreshIcons);

        if (setPosition || newProfile)
            setProfileSelection(profile);
    }

    /*
    public int getFilterType()
    {
        return filterType;
    }
    */

    private static void sortAlphabetically(List<Profile> profileList)
    {
        class AlphabeticallyComparator implements Comparator<Profile> {
            public int compare(Profile lhs, Profile rhs) {
                if (GlobalGUIRoutines.collator != null)
                    return GlobalGUIRoutines.collator.compare(lhs._name, rhs._name);
                else
                    return 0;
            }
        }
        Collections.sort(profileList, new AlphabeticallyComparator());
    }

    private static void sortByPOrder(List<Profile> profileList)
    {
        class ByPOrderComparator implements Comparator<Profile> {
            public int compare(Profile lhs, Profile rhs) {
                int res = 0;
                if ((lhs != null) && (rhs != null))
                    res =  lhs._porder - rhs._porder;
                return res;
            }
        }
        Collections.sort(profileList, new ByPOrderComparator());
    }

    public void refreshGUI(boolean refreshIcons, boolean setPosition)
    {
        if ((activityDataWrapper == null) || (profileListAdapter == null))
            return;

        PPApplication.logE("EditorProfileListFragment.refreshGUI", "refresh");

        Profile profileFromAdapter = profileListAdapter.getActivatedProfile();
        if (profileFromAdapter != null)
            profileFromAdapter._checked = false;

        Profile profileFromDB = DatabaseHandler.getInstance(activityDataWrapper.context).getActivatedProfile();
        if (profileFromDB != null) {
            PPApplication.logE("EditorProfileListFragment.refreshGUI", "profile activated");
            Profile profileFromDataWrapper = activityDataWrapper.getProfileById(profileFromDB._id, true,
                    ApplicationPreferences.applicationEditorPrefIndicator(activityDataWrapper.context), false);
            if (profileFromDataWrapper != null)
                profileFromDataWrapper._checked = true;
            updateHeader(profileFromDataWrapper);
            updateListView(profileFromDataWrapper, false, refreshIcons, setPosition);
        } else {
            PPApplication.logE("EditorProfileListFragment.refreshGUI", "profile not activated");
            updateHeader(null);
            updateListView(null, false, refreshIcons, setPosition);
        }
    }

    public void removeAdapter() {
        if (listView != null)
            listView.setAdapter(null);
    }

    void showTargetHelps() {
        /*if (Build.VERSION.SDK_INT <= 19)
            // TapTarget.forToolbarMenuItem FC :-(
            // Toolbar.findViewById() returns null
            return;*/

        if (getActivity() == null)
            return;

        if (((EditorProfilesActivity)getActivity()).targetHelpsSequenceStarted)
            return;

        ApplicationPreferences.getSharedPreferences(getActivity());

        boolean showTargetHelps = ApplicationPreferences.preferences.getBoolean(PREF_START_TARGET_HELPS, true);
        boolean showTargetHelpsDefaultProfile = ApplicationPreferences.preferences.getBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS_DEFAULT_PROFILE, true);
        if (showTargetHelps || showTargetHelpsDefaultProfile ||
                ApplicationPreferences.preferences.getBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS, true) ||
                ApplicationPreferences.preferences.getBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS_ORDER, true)) {

            //Log.d("EditorProfileListFragment.showTargetHelps", "PREF_START_TARGET_HELPS_ORDER=true");

            if (showTargetHelps || showTargetHelpsDefaultProfile) {

                //Log.d("EditorProfileListFragment.showTargetHelps", "PREF_START_TARGET_HELPS=true");

                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                editor.putBoolean(PREF_START_TARGET_HELPS, false);
                editor.putBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS_DEFAULT_PROFILE, false);
                editor.apply();

                int circleColor = R.color.tabTargetHelpCircleColor;
                if (ApplicationPreferences.applicationTheme(getActivity()).equals("dark"))
                    circleColor = R.color.tabTargetHelpCircleColor_dark;
                int textColor = R.color.tabTargetHelpTextColor;
                if (ApplicationPreferences.applicationTheme(getActivity()).equals("white"))
                    textColor = R.color.tabTargetHelpTextColor_white;
                boolean tintTarget = !ApplicationPreferences.applicationTheme(getActivity()).equals("white");

                final TapTargetSequence sequence = new TapTargetSequence(getActivity());
                List<TapTarget> targets = new ArrayList<>();
                int id = 1;
                if (showTargetHelps) {
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_add_profile, getString(R.string.editor_activity_targetHelps_newProfileButton_title), getString(R.string.editor_activity_targetHelps_newProfileButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_delete_all_profiles, getString(R.string.editor_activity_targetHelps_deleteAllProfilesButton_title), getString(R.string.editor_activity_targetHelps_deleteAllProfilesButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                }
                if (showTargetHelpsDefaultProfile) {
                    try {
                        targets.add(
                                TapTarget.forToolbarMenuItem(bottomToolbar, R.id.menu_default_profile, getString(R.string.editor_activity_targetHelps_backgroundProfileButton_title), getString(R.string.editor_activity_targetHelps_backgroundProfileButton_description))
                                        .targetCircleColor(circleColor)
                                        .textColor(textColor)
                                        .tintTarget(tintTarget)
                                        .drawShadow(true)
                                        .id(id)
                        );
                        ++id;
                    } catch (Exception ignored) {
                    } // not in action bar?
                }

                sequence.targets(targets)
                        .listener(new TapTargetSequence.Listener() {
                            // This listener will tell us when interesting(tm) events happen in regards
                            // to the sequence
                            @Override
                            public void onSequenceFinish() {
                                targetHelpsSequenceStarted = false;
                                showAdapterTargetHelps();
                            }

                            @Override
                            public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                                //Log.d("TapTargetView", "Clicked on " + lastTarget.id());
                            }

                            @Override
                            public void onSequenceCanceled(TapTarget lastTarget) {
                                targetHelpsSequenceStarted = false;
                                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                                editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS, false);
                                if (filterType == FILTER_TYPE_SHOW_IN_ACTIVATOR)
                                    editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS_ORDER, false);
                                editor.apply();
                            }
                        });
                sequence.continueOnCancel(true)
                        .considerOuterCircleCanceled(true);
                targetHelpsSequenceStarted = true;
                sequence.start();
            }
            else {
                //Log.d("EditorProfileListFragment.showTargetHelps", "PREF_START_TARGET_HELPS=false");
                final Handler handler = new Handler(getActivity().getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showAdapterTargetHelps();
                    }
                }, 500);
            }
        }
    }

    private void showAdapterTargetHelps() {
        /*if (Build.VERSION.SDK_INT <= 19)
            // TapTarget.forToolbarMenuItem FC :-(
            // Toolbar.findViewById() returns null
            return;*/

        if (getActivity() == null)
            return;

        View itemView;
        if (listView.getChildCount() > 1)
            itemView = listView.getChildAt(1);
        else
            itemView = listView.getChildAt(0);
        //Log.d("EditorProfileListFragment.showAdapterTargetHelps", "profileListAdapter="+profileListAdapter);
        //Log.d("EditorProfileListFragment.showAdapterTargetHelps", "itemView="+itemView);
        if ((profileListAdapter != null) && (itemView != null))
            profileListAdapter.showTargetHelps(getActivity(), this, itemView);
        else {
            targetHelpsSequenceStarted = false;
            ApplicationPreferences.getSharedPreferences(getActivity());
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putBoolean(PREF_START_TARGET_HELPS, false);
            editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS, false);
            editor.apply();
        }
    }

}
