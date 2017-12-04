package org.smartregister.bidan.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.opensrp.api.domain.Location;
import org.opensrp.api.util.EntityUtils;
import org.opensrp.api.util.LocationTree;
import org.opensrp.api.util.TreeNode;
import org.smartregister.Context;
import org.smartregister.bidan.R;
import org.smartregister.bidan.activity.DetailFPActivity;
//import org.smartregister.bidan.activity.LoginActivity;
import org.smartregister.bidan.activity.NativeKIFPSmartRegisterActivity;
import org.smartregister.bidan.activity.NativeKISmartRegisterActivity;
import org.smartregister.bidan.options.AllKBServiceMode;
import org.smartregister.bidan.options.MotherFilterOption;
import org.smartregister.bidan.provider.KBClientsProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.CursorCommonObjectFilterOption;
import org.smartregister.cursoradapter.CursorCommonObjectSort;
import org.smartregister.cursoradapter.SmartRegisterPaginatedCursorAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.util.StringUtil;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;
import org.smartregister.view.contract.ECClient;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.dialog.AllClientsFilter;
import org.smartregister.view.dialog.DialogOption;
import org.smartregister.view.dialog.DialogOptionModel;
import org.smartregister.view.dialog.EditOption;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.LocationSelectorDialogFragment;
import org.smartregister.view.dialog.NameSort;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.view.View.INVISIBLE;
import static org.smartregister.bidan.utils.AllConstantsINA.FormNames.KOHORT_KB_REGISTER;

/**
 * Created by sid-tech on 11/30/17.
 */

public class FPSmartRegisterFragment extends BaseSmartRegisterFragment {

    private static final String TAG = FPSmartRegisterFragment.class.getSimpleName();
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();
    private String locationDialogTAG = "locationDialogTAG";

    Date date = new Date();
    SimpleDateFormat sdf;
    Map<String, String> FS = new HashMap<>();

    String tableName = "ec_kartu_ibu";

    @Override
    protected void onCreation() {
    }

//    @Override
//    protected SmartRegisterPaginatedAdapter adapter() {
//        return new SmartRegisterPaginatedAdapter(clientsProvider());
//    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new AllKBServiceMode(clientsProvider());
            }

            @Override
            public FilterOption villageFilter() {
                return new AllClientsFilter();
            }

            @Override
            public SortOption sortOption() {
                return new NameSort();

            }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.kb_register_title_in_short);
            }
        };
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
//                FlurryFacade.logEvent("click_filter_option_on_kohort_kb_dashboard");
                ArrayList<DialogOption> dialogOptionslist = new ArrayList<>();

                dialogOptionslist.add(new CursorCommonObjectFilterOption(getString(R.string.filter_by_all_label),filterStringForAll()));

                String locationJSON = context().anmLocationController().get();
                LocationTree locationTree = EntityUtils.fromJson(locationJSON, LocationTree.class);

                Map<String,TreeNode<String, Location>> locationMap = locationTree.getLocationsHierarchy();
                addChildToList(dialogOptionslist,locationMap);
                DialogOption[] dialogOptions = new DialogOption[dialogOptionslist.size()];
                for (int i = 0;i < dialogOptionslist.size();i++){
                    dialogOptions[i] = dialogOptionslist.get(i);
                }

                return  dialogOptions;
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{};
            }

            @Override
            public DialogOption[] sortingOptions() {
//                FlurryFacade.logEvent("click_sorting_option_on_kohort_kb_dashboard");
                return new DialogOption[]{
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_name_label),KiSortByNameAZ()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_name_label_reverse),KiSortByNameZA()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_wife_age_label),KiSortByAge()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_edd_label),KiSortByEdd()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_no_ibu_label),KiSortByNoIbu()),
                };
            }

            @Override
            public String searchHint() {
                return getResources().getString(R.string.hh_search_hint);
            }
        };
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
        return null;
    }

    private DialogOption[] getEditOptions() {
        return ((NativeKIFPSmartRegisterActivity) getActivity()).getEditOptions();
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    public void setupViews(View view) {
        getDefaultOptionsProvider();

        super.setupViews(view);
        view.findViewById(R.id.btn_report_month).setVisibility(INVISIBLE);
        view.findViewById(R.id.service_mode_selection).setVisibility(View.GONE);
        view.findViewById(R.id.register_client).setVisibility(View.GONE);
        clientsView.setVisibility(View.VISIBLE);
        clientsProgressView.setVisibility(View.INVISIBLE);
        initializeQueries(getCriteria());
    }
    private String filterStringForAll(){
        return "";
    }

    private String sortByAlertmethod() {
        return "CASE WHEN alerts.status = 'urgent' THEN '1'" +
                "WHEN alerts.status = 'upcoming' THEN '2'\n" +
                "WHEN alerts.status = 'normal' THEN '3'\n" +
                "WHEN alerts.status = 'expired' THEN '4'\n" +
                "WHEN alerts.status is Null THEN '5'\n" +
                "Else alerts.status END ASC";
    }

    public String KartuIbuMainCount(){
        return "Select Count(*) from ec_kartu_ibu";
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void initializeQueries(String s){
        try {
            KBClientsProvider kbScp = new KBClientsProvider(getActivity(), clientActionHandler, context().alertService());
            clientAdapter = new SmartRegisterPaginatedCursorAdapter(getActivity(), null, kbScp,
                    new CommonRepository(
                            tableName,
                            new String []{
                                    "is_closed",
                                    "namalengkap",
                                    "umur",
                                    "namaSuami",
                                    "isOutOfArea"
                            }));

            clientsView.setAdapter(clientAdapter);

            setTablename(tableName);
            SmartRegisterQueryBuilder countqueryBuilder = new SmartRegisterQueryBuilder();
            countqueryBuilder.SelectInitiateMainTableCounts(tableName);
            //   countqueryBuilder.customJoin("LEFT JOIN ec_ibu on ec_kartu_ibu.id = ec_ibu.base_entity_id");


            if(s != null && !s.isEmpty()){
                Log.e(TAG, "initializeQueries with ID = " + s);
                mainCondition = "is_closed = 0 and jenisKontrasepsi != '0' AND object_id LIKE '%" + s + "%'";

            } else {
//                mainCondition = "is_closed = 0 and jenisKontrasepsi != '0' AND namalengkap != '' ";
//                mainCondition = "is_closed = 1 ";
                mainCondition = "";
                Log.e(TAG, "initializeQueries: Not Initialized");
            }

            joinTable = "";
            countSelect = countqueryBuilder.mainCondition(mainCondition);
            super.CountExecute();

            SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
            queryBuilder.SelectInitiateMainTable(
                    tableName,
                    new String[]{
                            tableName + ".relationalid",
                            tableName + ".is_closed",
                            tableName + ".details",
                            tableName + ".isOutOfArea",
                            "namalengkap",
                            "umur",
                            "namaSuami",
                    });

//            queryBuilder.customJoin("LEFT JOIN ec_ibu ON "+tableName+".id = ec_ibu.base_entity_id LEFT JOIN ImageList imagelist ON ec_ibu.base_entity_id=imagelist.entityID ");
            queryBuilder.customJoin("LEFT JOIN ec_ibu ON "+tableName+".id = ec_ibu.base_entity_id");

            mainSelect = queryBuilder.mainCondition(mainCondition);
            Sortqueries = KiSortByNameAZ();

            currentlimit = 20;
            currentoffset = 0;

            super.filterandSortInInitializeQueries();
            CountExecute();
            updateSearchView();

            refresh();

        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
        }

    }


    @Override
    public void startRegistration() {
        Log.e(TAG, "startRegistration: " );
//        FlurryFacade.logEvent("click_start_registration_on_kohort_kb_dashboard");
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        Fragment prev = getActivity().getFragmentManager().findFragmentByTag(locationDialogTAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

//        LocationSelectorDialogFragment
//                .newInstance(
//                        (NativeKIFPSmartRegisterActivity) getActivity(),
//                        new EditDialogOptionModel(), context().anmLocationController().get(),
//                        KOHORT_KB_REGISTER)
//                .show(ft, locationDialogTAG);

        LocationSelectorDialogFragment
                .newInstance((NativeKISmartRegisterActivity) getActivity(),
                        ((NativeKISmartRegisterActivity)getActivity()).new EditDialogOptionModel(), context().anmLocationController().get(),
                        KOHORT_KB_REGISTER)
                .show(ft, locationDialogTAG);

    }

    private class ClientActionHandler implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.profile_info_layout:
//                    FlurryFacade.logEvent("click_detail_view_on_kohort_kb_dashboard");
                    DetailFPActivity.kiclient = (CommonPersonObjectClient)view.getTag();
                    Intent intent = new Intent(getActivity(),DetailFPActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                    break;
                case R.id.btn_edit:
//                    FlurryFacade.logEvent("click_visit_button_on_kohort_kb_dashboard");
//                    showFragmentDialog(new EditDialogOptionModel(), view.getTag());
                    showFragmentDialog(((NativeKIFPSmartRegisterActivity) getActivity()).new EditDialogOptionModel(), view.getTag());

                    break;
            }
        }

        private void showProfileView(ECClient client) {
            navigationController.startEC(client.entityId());
        }
    }

    private String KiSortByNameAZ() {
        return "namalengkap ASC";
    }

    private String KiSortByNameZA() {
        return "namalengkap DESC";
    }

    private String KiSortByAge() {
        return "umur DESC";
    }

    private String KiSortByNoIbu() {
        return "noIbu ASC";
    }

    private String KiSortByEdd() {
        return "htp IS NULL, htp";
    }

    private class EditDialogOptionModelOld implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return getEditOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onEditSelection((EditOption) option, (SmartRegisterClient) tag);
        }
    }

    @Override
    protected void onResumption() {
//        super.onResumption();
        getDefaultOptionsProvider();
        if(isPausedOrRefreshList()) {
            initializeQueries("!");
        }
        //     updateSearchView();
        //   checkforNidMissing(mView);
//
//        try{
//            LoginActivity.setLanguage();
//        }catch (Exception e){
//
//        }

    }

    private void updateSearchView() {
        getSearchView().removeTextChangedListener(textWatcher);
        getSearchView().addTextChangedListener(textWatcher);
    }

    public void addChildToList(ArrayList<DialogOption> dialogOptionslist, Map<String,TreeNode<String, Location>> locationMap){
        for(Map.Entry<String, TreeNode<String, Location>> entry : locationMap.entrySet()) {

            if(entry.getValue().getChildren() != null) {
                addChildToList(dialogOptionslist,entry.getValue().getChildren());

            }else{
                StringUtil.humanize(entry.getValue().getLabel());
                String name = StringUtil.humanize(entry.getValue().getLabel());
                dialogOptionslist.add(new MotherFilterOption(name, "location_name", name, tableName ));

            }
        }
    }

    //    WD
    public static String criteria;

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public static String getCriteria() {
        return criteria;
    }

    //    WD
    @Override
    public void setupSearchView(final View view) {
        searchView = (EditText) view.findViewById(R.id.edt_search);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (SmartShutterActivity.isDevCompat) {
//                    CharSequence selections[] = new CharSequence[]{"Name", "Photo"};
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                    builder.setTitle("Please Choose one, Search by");
//                    builder.setItems(selections, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int opt) {
//                            if (opt == 0) searchTextChangeListener("");
//                            else getFacialRecord(view);
//                        }
//                    });
//                    builder.show();
//                } else {
                    searchTextChangeListener("");
//                }
            }
        });

        searchCancelView = view.findViewById(R.id.btn_search_cancel);
        searchCancelView.setOnClickListener(searchCancelHandler);
    }

    public void getFacialRecord(View view) {
//        FlurryAgent.logEvent(TAG+" search_by_face", true);
//        Log.e(TAG, "getFacialRecord: ");
//        sdf = new SimpleDateFormat("hh:mm:ss.SS", Locale.ENGLISH);
//        String face_start = sdf.format(date);
//        FS.put("face_start", face_start);
//        SmartShutterActivity.kidetail = (CommonPersonObjectClient) view.getTag();
//        FlurryAgent.logEvent(TAG + " search_by_face", FS, true);
//
//        Intent intent = new Intent(getActivity(), SmartShutterActivity.class);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.origin", TAG);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.identify", true);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.kidetail", (Parcelable) SmartShutterActivity.kidetail);
//        startActivityForResult(intent, 2);
    }

    public void searchTextChangeListener(String s) {
        Log.e(TAG, "searchTextChangeListener: " + s);
        if (s != null) {
            filters = s;
        } else {
            searchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                @Override
                public void onTextChanged(final CharSequence cs, int start, int before, int count) {

                    (new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] params) {
                            filters = cs.toString();
                            joinTable = "";
                            mainCondition = "isClosed !='true' and ibuCaseId !='' ";
                            return null;
                        }
                    }).execute();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        Intent myIntent = new Intent(getActivity(), NativeKIFPSmartRegisterActivity.class);
        if (data != null) {
            myIntent.putExtra("indonesia.face.face_mode", true);
            myIntent.putExtra("indonesia.face.base_id", data.getStringExtra("indonesia.face.base_id"));
        }
        getActivity().startActivity(myIntent);

    }

}
