package org.smartregister.bidan.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.adapter.SmartRegisterPaginatedAdapter;
import org.smartregister.bidan.R;
import org.smartregister.bidan.adapter.BidanRegisterActivityPagerAdapter;
import org.smartregister.bidan.fragment.AdvancedSearchFragment;
import org.smartregister.bidan.fragment.BaseSmartRegisterFragment;
import org.smartregister.bidan.fragment.KKBSmartRegisterFragment;
import org.smartregister.bidan.view.LocationPickerView;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.form.FormSubmission;
import org.smartregister.event.Event;
import org.smartregister.event.Listener;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.service.FormSubmissionService;
import org.smartregister.service.ZiggyService;
import org.smartregister.util.FormUtils;
import org.smartregister.view.dialog.DialogOptionModel;
import org.smartregister.view.viewpager.OpenSRPViewPager;

import butterknife.Bind;
import butterknife.ButterKnife;
import util.JsonFormUtils;
import util.barcode.Barcode;
import util.barcode.BarcodeIntentIntegrator;
import util.barcode.BarcodeIntentResult;

import static android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS;

public class KKBSmartRegisterActivity extends BaseRegisterActivity {
    private static final String TAG = KKBSmartRegisterActivity.class.getCanonicalName();

    @Bind(R.id.view_pager)
    protected OpenSRPViewPager mPager;
    private FragmentPagerAdapter mPagerAdapter;
    private static final int REQUEST_CODE_GET_JSON = 3432;
    private int currentPage;
    public static final int ADVANCED_SEARCH_POSITION = 1;
    private ProgressDialog progressDialog;

    private Fragment mBaseFragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mBaseFragment = new KKBSmartRegisterFragment();
        Fragment[] otherFragments = {new AdvancedSearchFragment()};

        // Instantiate a ViewPager and a PagerAdapter.
        mPagerAdapter = new BidanRegisterActivityPagerAdapter(getSupportFragmentManager(), mBaseFragment, otherFragments);
        mPager.setOffscreenPageLimit(otherFragments.length);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
            }
        });

        Event.ON_DATA_FETCHED.addListener(onDataFetchedListener);
        initializeProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Event.ON_DATA_FETCHED.removeListener(onDataFetchedListener);
    }

    @Override
    protected SmartRegisterPaginatedAdapter adapter() {
        return new SmartRegisterPaginatedAdapter(clientsProvider());
    }

    @Override
    protected DefaultOptionsProvider getDefaultOptionsProvider() {
        return null;
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void onResumption() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout childregister = (LinearLayout) drawer.findViewById(R.id.child_register);
        childregister.setBackgroundColor(getResources().getColor(R.color.tintcolor));

    }

    @Override
    protected NavBarOptionsProvider getNavBarOptionsProvider() {
        return null;
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        return null;
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    public void startRegistration() {
    }

    @Override
    public void showFragmentDialog(DialogOptionModel dialogOptionModel, Object tag) {
        try {
            LoginActivity.setLanguage();
        } catch (Exception e) {
            Log.e(getClass().getCanonicalName(), e.getMessage());
        }
        super.showFragmentDialog(dialogOptionModel, tag);
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        try {
            if (mBaseFragment instanceof KKBSmartRegisterFragment) {
                LocationPickerView locationPickerView = ((KKBSmartRegisterFragment) mBaseFragment).getLocationPickerView();
                String locationId = JsonFormUtils.getOpenMrsLocationId(context(), locationPickerView.getSelectedItem());
                JsonFormUtils.startForm(this, context(), REQUEST_CODE_GET_JSON, formName, entityId,
                        metaData, locationId);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }

    public void startAdvancedSearch() {
        try {
            mPager.setCurrentItem(ADVANCED_SEARCH_POSITION, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateAdvancedSearchFilterCount(int count) {
        AdvancedSearchFragment advancedSearchFragment = (AdvancedSearchFragment) findFragmentByPosition(ADVANCED_SEARCH_POSITION);
        if (advancedSearchFragment != null) {
            advancedSearchFragment.updateFilterCount(count);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON) {
            if (resultCode == RESULT_OK) {

                String jsonString = data.getStringExtra("json");
                Log.d("JSONResult", jsonString);

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);

                JsonFormUtils.saveForm(this, context(), jsonString, allSharedPreferences.fetchRegisteredANM());
            }
        } else if (requestCode == BarcodeIntentIntegrator.REQUEST_CODE && resultCode == RESULT_OK) {
            BarcodeIntentResult res = BarcodeIntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (StringUtils.isNotBlank(res.getContents())) {
                onQRCodeSucessfullyScanned(res.getContents());
            } else Log.i("", "NO RESULT FOR QR CODE");
        }
    }

    @Override
    public void saveFormSubmission(String formSubmission, String id, String formName, JSONObject fieldOverrides) {
        // save the form
        try {
            FormUtils formUtils = FormUtils.getInstance(getApplicationContext());
            FormSubmission submission = formUtils.generateFormSubmisionFromXMLString(id, formSubmission, formName, fieldOverrides);

            org.smartregister.Context context = context();
            ZiggyService ziggyService = context.ziggyService();
            ziggyService.saveForm(getParams(submission), submission.instance());

            FormSubmissionService formSubmissionService = context.formSubmissionService();
            formSubmissionService.updateFTSsearch(submission);

            Log.v("we are here", "hhregister");
            //switch to forms list fragmentstregi
            switchToBaseFragment(formSubmission); // Unnecessary!! passing on data

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void switchToBaseFragment(final String data) {
        Log.v("we are here", "switchtobasegragment");
        final int prevPageIndex = currentPage;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPager.setCurrentItem(0, false);
                refreshList(data);
            }
        });
    }

    @Override
    public void onBackPressed() {
        BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(currentPage);
        if (registerFragment.onBackPressed()) {
            return;
        }
        if (currentPage != 0) {
            new AlertDialog.Builder(this, R.style.PathAlertDialog)
                    .setMessage(R.string.form_back_confirm_dialog_message)
                    .setTitle(R.string.form_back_confirm_dialog_title)
                    .setCancelable(false)
                    .setPositiveButton(R.string.no_button_label,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                }
                            })
                    .setNegativeButton(R.string.yes_button_label,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    switchToBaseFragment(null);
                                }
                            })
                    .show();
        } else if (currentPage == 0) {
            super.onBackPressed(); // allow back key only if we are
        }
    }

    public Fragment findFragmentByPosition(int position) {
        FragmentPagerAdapter fragmentPagerAdapter = mPagerAdapter;
        return getSupportFragmentManager().findFragmentByTag("android:switcher:" + mPager.getId() + ":" + fragmentPagerAdapter.getItemId(position));
    }

    private boolean currentActivityIsShowingForm() {
        return currentPage != 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void startQrCodeScanner() {
        BarcodeIntentIntegrator integ = new BarcodeIntentIntegrator(this);
        integ.addExtra(Barcode.SCAN_MODE, Barcode.QR_MODE);
        integ.initiateScan();
    }

    public void filterSelection() {
        if (currentPage != 0) {
            switchToBaseFragment(null);
            BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(0);
            if (registerFragment != null && registerFragment instanceof KKBSmartRegisterFragment) {
                ((KKBSmartRegisterFragment) registerFragment).triggerFilterSelection();
            }
        }
    }

    private void onQRCodeSucessfullyScanned(String qrCode) {
        Log.i(getClass().getName(), "QR code: " + qrCode);
        if (StringUtils.isNotBlank(qrCode)) {
            filterList(qrCode.replace("-", ""));
        }
    }

    private final Listener<FetchStatus> onDataFetchedListener = new Listener<FetchStatus>() {
        @Override
        public void onEvent(FetchStatus fetchStatus) {
            refreshList(fetchStatus);
        }
    };

    public void refreshList(final FetchStatus fetchStatus) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(0);
            if (registerFragment != null && fetchStatus.equals(FetchStatus.fetched)) {
                registerFragment.refreshListView();
            }
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(0);
                    if (registerFragment != null && fetchStatus.equals(FetchStatus.fetched)) {
                        registerFragment.refreshListView();
                    }
                }
            });
        }

    }

    private void refreshList(String data) {
        BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(0);
        if (registerFragment != null && data != null) {
            registerFragment.refreshListView();
        }

    }

    private void filterList(String filterString) {
        BaseSmartRegisterFragment registerFragment = (BaseSmartRegisterFragment) findFragmentByPosition(0);
        if (registerFragment != null) {
            registerFragment.openVaccineCard(filterString);
        }
    }

    public void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), HIDE_NOT_ALWAYS);
    }

    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.saving_dialog_title));
        progressDialog.setMessage(getString(R.string.please_wait_message));
    }

    public void showProgressDialog(String title, String message) {
        if (progressDialog != null) {
            if (StringUtils.isNotBlank(title)) {
                progressDialog.setTitle(title);
            }

            if (StringUtils.isNotBlank(message)) {
                progressDialog.setMessage(message);
            }

            progressDialog.show();
        }
    }

    public void showProgressDialog() {
        showProgressDialog(getString(R.string.saving_dialog_title), getString(R.string.please_wait_message));
    }

    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
