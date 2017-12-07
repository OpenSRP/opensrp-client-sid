package org.smartregister.bidan.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.bidan.R;
import org.smartregister.bidan.pageradapter.BaseRegisterActivityPagerAdapter;
import org.smartregister.bidan.sync.ClientProcessor;
import org.smartregister.bidan.utils.BidanFormUtils;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.form.FieldOverrides;
import org.smartregister.domain.form.FormSubmission;
import org.smartregister.enketo.listener.DisplayFormListener;
import org.smartregister.enketo.view.fragment.DisplayFormFragment;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.dialog.DialogOption;
import org.smartregister.view.dialog.DialogOptionModel;
import org.smartregister.view.dialog.EditOption;
import org.smartregister.view.dialog.OpenFormOption;
import org.smartregister.view.fragment.SecuredNativeSmartRegisterFragment;
import org.smartregister.view.viewpager.OpenSRPViewPager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_ANC_CLOSE;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_ANC_RENCANA_PERSALINAN;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_ANC_VISIT;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_ANC_VISIT_INTEGRASI;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_ANC_VISIT_LABTEST;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KARTU_IBU_PNC_REGISTRATION;
import static org.smartregister.util.Utils.getValue;

/**
 * Created by sid-tech on 12/7/17.
 */

public class BaseRegisterActivity extends SecuredNativeSmartRegisterActivity implements DisplayFormListener {

    protected SimpleDateFormat timer = new SimpleDateFormat("hh:mm:ss");

    @Bind(R.id.view_pager)
    protected OpenSRPViewPager mPager;

    protected int currentPage;
    protected FragmentPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use core layout
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mPagerAdapter = new BaseRegisterActivityPagerAdapter(getSupportFragmentManager(), formNames(), mBaseFragment());
        mPager.setOffscreenPageLimit(formNames().length);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                onPageChanged(position);
            }
        });

        ziggyService = context().ziggyService();


    }

    protected String[] formNames() {

        return null;
    }

    protected Fragment mBaseFragment(){

        return null;
    }

    @Override
    protected DefaultOptionsProvider getDefaultOptionsProvider() {
        return null;
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
    protected void setupViews() {

    }

    @Override
    protected void onResumption() {
    }

    @Override
    protected void onPause() {
        super.onPause();
        retrieveAndSaveUnsubmittedFormData();

        String KIEnd = timer.format(new Date());
        Map<String, String> KI = new HashMap<>();
        KI.put("end", KIEnd);
    }

    @Override
    public void onBackPressed() {
//        nf.setCriteria("");
//        Log.e(TAG, "onBackPressed: "+currentPage );

        if (currentPage != 0) {
            switchToBaseFragment(null);
        } else {
            super.onBackPressed(); // allow back key only if we are
        }
    }

    public void switchToBaseFragment(final String data){
        final int prevPageIndex = currentPage;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPager.setCurrentItem(0, false);
                SecuredNativeSmartRegisterFragment registerFragment = (SecuredNativeSmartRegisterFragment) findFragmentByPosition(0);
                if (registerFragment != null && data != null) {
                    registerFragment.refreshListView();
                }

                //hack reset the form
                DisplayFormFragment displayFormFragment = getDisplayFormFragmentAtIndex(prevPageIndex);
                if (displayFormFragment != null) {
                    displayFormFragment.hideTranslucentProgressDialog();
                    displayFormFragment.setFormData(null);

                }

                displayFormFragment.setRecordId(null);
            }
        });

    }


    public void onPageChanged(int page){
        setRequestedOrientation(page == 0 ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private boolean currentActivityIsShowingForm() {
        return currentPage != 0;
    }

    public DisplayFormFragment getDisplayFormFragmentAtIndex(int index) {
        return (DisplayFormFragment) findFragmentByPosition(index);
    }

    public void retrieveAndSaveUnsubmittedFormData() {
        if (currentActivityIsShowingForm()) {
            DisplayFormFragment formFragment = getDisplayFormFragmentAtIndex(currentPage);
            formFragment.saveCurrentFormData();
        }
    }

    public Fragment findFragmentByPosition(int position) {
        FragmentPagerAdapter fragmentPagerAdapter = mPagerAdapter;
        return getSupportFragmentManager().findFragmentByTag("android:switcher:" + mPager.getId() + ":" + fragmentPagerAdapter.getItemId(position));
    }

    public void saveuniqueid() {
//        Log.e(TAG, "saveuniqueid: saved" );
//        try {
//            JSONObject uniqueId = new JSONObject(LoginActivity.generator.uniqueIdController().getUniqueIdJson());
//            String uniq = uniqueId.getString("unique_id");
//            LoginActivity.generator.uniqueIdController().updateCurrentUniqueId(uniq);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void startFormActivity(String formName, String entityId, String metaData) {
        //  FlurryFacade.logEvent(formName);
//        if(Support.ONSYNC) {
//            Toast.makeText(this,"Data still Synchronizing, please wait",Toast.LENGTH_SHORT).show();
//            return;
//        }
        String start = timer.format(new Date());
        Map<String, String> FS = new HashMap<>();
        FS.put("start", start);
//        FlurryAgent.logEvent(formName,FS, true );
//        Log.v("fieldoverride", metaData);
        try {
            int formIndex = BidanFormUtils.getIndexForFormName(formName, formNames()) + 1; // add the offset
            if (entityId != null || metaData != null){
                String data = null;
                //check if there is previously saved data for the form
                data = getPreviouslySavedDataForForm(formName, metaData, entityId);
                if (data == null){
                    data = BidanFormUtils.getInstance(getApplicationContext()).generateXMLInputForFormWithEntityId(entityId, formName, metaData);
                }

                DisplayFormFragment displayFormFragment = getDisplayFormFragmentAtIndex(formIndex);
                if (displayFormFragment != null) {
                    displayFormFragment.setFormData(data);
                    displayFormFragment.setRecordId(entityId);
                    displayFormFragment.setFieldOverides(metaData);

                    displayFormFragment.setListener(this);

                }
            }

            mPager.setCurrentItem(formIndex, false); //Don't animate the view on orientation change the view disapears

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void saveFormSubmission(String formSubmission, String id, String formName, JSONObject fieldOverrides){
        Log.e("fieldoverride", formSubmission);
        Log.e("fieldoverride", formName);
        Log.e("fieldoverride", fieldOverrides.toString());

        // save the form
        try {
            BidanFormUtils formUtils = BidanFormUtils.getInstance(getApplicationContext());
            FormSubmission submission = formUtils.generateFormSubmisionFromXMLString(id, formSubmission, formName, fieldOverrides);

            Log.e(TAG, "saveFormSubmission: "+ submission );

            ziggyService.saveForm(getParams(submission), submission.instance());
            ClientProcessor.getInstance(getApplicationContext()).processClient();

            context().formSubmissionService().updateFTSsearch(submission);
            context().formSubmissionRouter().handleSubmission(submission, formName);

            if(formName.equals("kartu_ibu_registration")){
                saveuniqueid();
            }
            //switch to forms list fragment
            switchToBaseFragment(formSubmission); // Unnecessary!! passing on data

        } catch (Exception e){
            // TODO: show error dialog on the formfragment if the submission fails
            DisplayFormFragment displayFormFragment = getDisplayFormFragmentAtIndex(currentPage);
            if (displayFormFragment != null) {
                displayFormFragment.hideTranslucentProgressDialog();
            }
            e.printStackTrace();
        }
        //end capture flurry log for FS
        String end = timer.format(new Date());
        Map<String, String> FS = new HashMap<String, String>();
        FS.put("end", end);
//        FlurryAgent.logEvent(formName,FS, true);
    }

    public DialogOption[] getEditOptions() {
        return new DialogOption[]{};
    }


    public class EditDialogOptionModel implements DialogOptionModel {

        @Override
        public DialogOption[] getDialogOptions() {
            return getEditOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            CommonPersonObjectClient pc = (CommonPersonObjectClient) tag;
            DetailsRepository detailsRepository = org.smartregister.Context.getInstance().detailsRepository();
            detailsRepository.updateDetails(pc);
            String ibuCaseId = getValue(pc.getColumnmaps(), "relational_id", true).toLowerCase();
            Log.d(TAG, "onDialogOptionSelection: "+pc.getDetails());
            JSONObject fieldOverrides = new JSONObject();
            try {
                fieldOverrides.put("Province", pc.getDetails().get("stateProvince"));
                fieldOverrides.put("District", pc.getDetails().get("countyDistrict"));
                fieldOverrides.put("Sub-district", pc.getDetails().get("address2"));
                fieldOverrides.put("Village", pc.getDetails().get("cityVillage"));
                fieldOverrides.put("Sub-village", pc.getDetails().get("address1"));
                fieldOverrides.put("jenis_kelamin", pc.getDetails().get("gender"));
                fieldOverrides.put("ibuCaseId", ibuCaseId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            FieldOverrides fo = new FieldOverrides(fieldOverrides.toString());
            onEditSelectionWithMetadata((EditOption) option, (SmartRegisterClient) tag, fo.getJSONString());
        }
    }

    private String getDetailsPc(Object tag, String key) {
        CommonPersonObjectClient pc = (CommonPersonObjectClient) tag;

        return pc.getDetails().get(key);
    }


}
