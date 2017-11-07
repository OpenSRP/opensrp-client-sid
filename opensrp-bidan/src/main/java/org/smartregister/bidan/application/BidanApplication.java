package org.smartregister.bidan.application;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.bidan.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.bidan.repos.BidanRepository;
import org.smartregister.bidan.repos.UniqueIdBidanRepository;
import org.smartregister.bidan.repository.HIA2IndicatorsRepository;
import org.smartregister.bidan.repository.MonthlyTalliesRepository;
import org.smartregister.bidan.repository.StockRepository;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.growthmonitoring.GrowthMonitoringLibrary;
import org.smartregister.growthmonitoring.repository.WeightRepository;
import org.smartregister.growthmonitoring.repository.ZScoreRepository;
//import org.smartregister.immunization.ImmunizationLibrary;
//import org.smartregister.immunization.db.VaccineRepo;
//import org.smartregister.immunization.domain.VaccineSchedule;
//import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
//import org.smartregister.immunization.repository.RecurringServiceTypeRepository;
import org.smartregister.immunization.repository.VaccineNameRepository;
import org.smartregister.immunization.repository.VaccineRepository;
import org.smartregister.immunization.repository.VaccineTypeRepository;
//import org.smartregister.immunization.util.VaccinateActionUtils;
//import org.smartregister.immunization.util.VaccinatorUtils;
import org.smartregister.bidan.BuildConfig;
import org.smartregister.bidan.R;
import org.smartregister.bidan.activity.LoginActivity;
import org.smartregister.bidan.receiver.BidanSyncBroadcastReceiver;
//import org.smartregister.bidan.receiver.VaccinatorAlarmReceiver;
//import org.smartregister.bidan.repository.DailyTalliesRepository;
//import org.smartregister.bidan.repository.PathRepository;
//import org.smartregister.bidan.repository.UniqueIdRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.DrishtiSyncScheduler;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.receiver.TimeChangedBroadcastReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import util.BidanConstants;
import util.PathConstants;

import static org.smartregister.util.Log.logError;
import static org.smartregister.util.Log.logInfo;

/**
 * Created by koros on 2/3/16.
 */
public class BidanApplication extends DrishtiApplication
        implements TimeChangedBroadcastReceiver.OnTimeChangedListener {

    private static final String TAG = "BidanApplication";
    private static CommonFtsObject commonFtsObject;
    private EventClientRepository eventClientRepository;
    private UniqueIdBidanRepository uniqueIdRepository;
//    private UniqueIdRepository uniqueIdRepository;
//    private DailyTalliesRepository dailyTalliesRepository;
//    private MonthlyTalliesRepository monthlyTalliesRepository;
//    private HIA2IndicatorsRepository hIA2IndicatorsRepository;
//    private StockRepository stockRepository;
    private boolean lastModified;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        context = Context.getInstance();
        context.updateApplicationContext(getApplicationContext());
        context.updateCommonFtsObject(createCommonFtsObject());

        //Initialize Modules
        CoreLibrary.init(context());

//        GrowthMonitoringLibrary.init(context(), getRepository());
//        ImmunizationLibrary.init(context(), getRepository(), createCommonFtsObject());

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
        DrishtiSyncScheduler.setReceiverClass(BidanSyncBroadcastReceiver.class);

//        Hia2ServiceBroadcastReceiver.init(this);
        SyncStatusBroadcastReceiver.init(this);
        TimeChangedBroadcastReceiver.init(this);
        TimeChangedBroadcastReceiver.getInstance().addOnTimeChangedListener(this);

        applyUserLanguagePreference();
        cleanUpSyncState();
        initOfflineSchedules();
        setCrashlyticsUser(context);
//        setAlarms(this);

    }

    public static synchronized BidanApplication getInstance() {
        return (BidanApplication) mInstance;
    }

    @Override
    public void logoutCurrentUser() {

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getApplicationContext().startActivity(intent);
        context.userService().logoutSession();
    }

    protected void cleanUpSyncState() {
        DrishtiSyncScheduler.stop(getApplicationContext());
        context.allSharedPreferences().saveIsSyncInProgress(false);
    }


    @Override
    public void onTerminate() {
        logInfo("Application is terminating. Stopping Bidan Sync scheduler and resetting isSyncInProgress setting.");
        cleanUpSyncState();
        SyncStatusBroadcastReceiver.destroy(this);
        TimeChangedBroadcastReceiver.destroy(this);
        super.onTerminate();
    }

    protected void applyUserLanguagePreference() {
        Configuration config = getBaseContext().getResources().getConfiguration();

        String lang = context.allSharedPreferences().fetchLanguagePreference();
        if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
            locale = new Locale(lang);
            updateConfiguration(config);
        }
    }

    private void updateConfiguration(Configuration config) {
        config.locale = locale;
        Locale.setDefault(locale);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    private static String[] getFtsSearchFields(String tableName) {
        if (tableName.equals(PathConstants.CHILD_TABLE_NAME)) {
            return new String[]{"zeir_id", "epi_card_number", "first_name", "last_name"};
        } else if (tableName.equals(PathConstants.MOTHER_TABLE_NAME)) {
            return new String[]{"zeir_id", "epi_card_number", "first_name", "last_name", "father_name", "husband_name", "contact_phone_number"};
        }
        return null;
    }

    private static String[] getFtsSortFields(String tableName) {


        if (tableName.equals(BidanConstants.CHILD_TABLE_NAME)) {
            List<String> names = new ArrayList<>();
//            ArrayList<VaccineRepo.Vaccine> vaccines = VaccineRepo.getVaccines("child");
//            names.add("first_name");
//            names.add("dob");
//            names.add("zeir_id");
//            names.add("last_interacted_with");
//            names.add("inactive");
//            names.add("lost_to_follow_up");
//            names.add(PathConstants.EC_CHILD_TABLE.DOD);
//
//            for (VaccineRepo.Vaccine vaccine : vaccines) {
//                names.add("alerts." + VaccinateActionUtils.addHyphen(vaccine.display()));
//            }

            return names.toArray(new String[names.size()]);

        } else if (tableName.equals(PathConstants.MOTHER_TABLE_NAME)) {
            return new String[]{"first_name", "dob", "zeir_id", "last_interacted_with"};
        }
        return null;
    }

    private static String[] getFtsTables() {
        return new String[]{PathConstants.CHILD_TABLE_NAME, PathConstants.MOTHER_TABLE_NAME};
    }

    private static Map<String, Pair<String, Boolean>> getAlertScheduleMap() {
        Map<String, Pair<String, Boolean>> map = new HashMap<>();
//        ArrayList<VaccineRepo.Vaccine> vaccines = VaccineRepo.getVaccines("child");
//        for (VaccineRepo.Vaccine vaccine : vaccines) {
//            map.put(vaccine.display(), Pair.create(PathConstants.CHILD_TABLE_NAME, false));
//        }
        return map;
    }

    public static CommonFtsObject createCommonFtsObject() {
        if (commonFtsObject == null) {
            commonFtsObject = new CommonFtsObject(getFtsTables());
            for (String ftsTable : commonFtsObject.getTables()) {
                commonFtsObject.updateSearchFields(ftsTable, getFtsSearchFields(ftsTable));
                commonFtsObject.updateSortFields(ftsTable, getFtsSortFields(ftsTable));
            }
        }
        commonFtsObject.updateAlertScheduleMap(getAlertScheduleMap());
        return commonFtsObject;
    }

    /**
     * This method sets the Crashlytics user to whichever username was used to log in last. It only
     * does so if the app is not built for debugging
     *
     * @param context The user's context
     */
    public static void setCrashlyticsUser(Context context) {
        if (!BuildConfig.DEBUG
                && context != null && context.userService() != null
                && context.userService().getAllSharedPreferences() != null) {
            Crashlytics.setUserName(context.userService().getAllSharedPreferences().fetchRegisteredANM());
        }
    }

    private void grantPhotoDirectoryAccess() {
        Uri uri = FileProvider.getUriForFile(this,
                "com.vijay.jsonwizard.fileprovider",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        grantUriPermission("com.vijay.jsonwizard", uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @Override
    public Repository getRepository() {
        Log.e(TAG, "getRepository: " );
        try {
            if (repository == null) {
                repository = new BidanRepository(getInstance().getApplicationContext(), context());
                uniqueIdRepository();
                eventClientRepository();

//                dailyTalliesRepository();
//                monthlyTalliesRepository();
//                hIA2IndicatorsRepository();
//                stockRepository();
            }
        } catch (UnsatisfiedLinkError e) {
            logError("Error on getRepository: " + e);

        }
        return repository;
    }


//    public WeightRepository weightRepository() {
//        return GrowthMonitoringLibrary.getInstance().weightRepository();
//    }

    public Context context() {
        return context;
    }

//    public VaccineRepository vaccineRepository() {
//        return ImmunizationLibrary.getInstance().vaccineRepository();
//    }

//    public ZScoreRepository zScoreRepository() {
//        return GrowthMonitoringLibrary.getInstance().zScoreRepository();
//    }

    public UniqueIdBidanRepository uniqueIdRepository() {
        if (uniqueIdRepository == null) {
            uniqueIdRepository = new UniqueIdBidanRepository((BidanRepository) getRepository());
        }
        return uniqueIdRepository;
    }

//    public RecurringServiceTypeRepository recurringServiceTypeRepository() {
//        return ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
//    }
//
//    public RecurringServiceRecordRepository recurringServiceRecordRepository() {
//        return ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
//    }

    public EventClientRepository eventClientRepository() {
        if (eventClientRepository == null) {
            eventClientRepository = new EventClientRepository(getRepository());
        }
        return eventClientRepository;
    }

    public boolean isLastModified() {
        return lastModified;
    }

    public void setLastModified(boolean lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public void onTimeChanged() {
        Toast.makeText(this, R.string.device_time_changed, Toast.LENGTH_LONG).show();
        context.userService().forceRemoteLogin();
//        logoutCurrentUser();
    }

    @Override
    public void onTimeZoneChanged() {
        Toast.makeText(this, R.string.device_timezone_changed, Toast.LENGTH_LONG).show();
        context.userService().forceRemoteLogin();
        logoutCurrentUser();
    }

    private void initOfflineSchedules() {
        try {
//            JSONArray childVaccines = new JSONArray(VaccinatorUtils.getSupportedVaccines(this));
//            JSONArray specialVaccines = new JSONArray(VaccinatorUtils.getSpecialVaccines(this));
//            VaccineSchedule.init(childVaccines, specialVaccines, "child");
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void setAlarms(android.content.Context context) {
        final int TRIGGER_ITERATION_TWO_MINUTES = 2;
        final int TRIGGER_ITERATION_FIVE_MINUTES = 5;

//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_TWO_MINUTES, PathConstants.ServiceType.DAILY_TALLIES_GENERATION);
//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_TWO_MINUTES, PathConstants.ServiceType.WEIGHT_SYNC_PROCESSING);
//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_TWO_MINUTES, PathConstants.ServiceType.VACCINE_SYNC_PROCESSING);
//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_TWO_MINUTES, PathConstants.ServiceType.RECURRING_SERVICES_SYNC_PROCESSING);
//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_TWO_MINUTES, PathConstants.ServiceType.IMAGE_UPLOAD);
//        VaccinatorAlarmReceiver.setAlarm(context, TRIGGER_ITERATION_FIVE_MINUTES, PathConstants.ServiceType.PULL_UNIQUE_IDS);

    }

}
