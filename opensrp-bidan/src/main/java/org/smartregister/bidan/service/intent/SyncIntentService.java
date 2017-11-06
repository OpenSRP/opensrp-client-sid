package org.smartregister.bidan.service.intent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.bidan.application.BidanApplication;
import org.smartregister.bidan.domain.Stock;
import org.smartregister.bidan.repository.StockRepository;
import org.smartregister.bidan.sync.PathAfterFetchListener;
import org.smartregister.bidan.sync.PathClientProcessor;
import org.smartregister.domain.DownloadStatus;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Response;
import org.smartregister.growthmonitoring.service.intent.ZScoreRefreshIntentService;
import org.smartregister.bidan.R;
import org.smartregister.bidan.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.bidan.sync.ECSyncUpdater;
import org.smartregister.bidan.view.LocationPickerView;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.service.ActionService;
import org.smartregister.service.AllFormVersionSyncService;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.Utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import util.NetworkUtils;

public class SyncIntentService extends IntentService {
    private static final String EVENTS_SYNC_PATH = "/rest/event/add";
    private static final String REPORTS_SYNC_PATH = "/rest/report/add";
    private static final String STOCK_Add_PATH = "/rest/stockresource/add/";
    private static final String STOCK_SYNC_PATH = "rest/stockresource/sync/";

    private Context context;
    private ActionService actionService;
    private AllFormVersionSyncService allFormVersionSyncService;
    private HTTPAgent httpAgent;
    private PathAfterFetchListener pathAfterFetchListener;
    private static final int EVENT_FETCH_LIMIT = 50;

    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getBaseContext();
        actionService = BidanApplication.getInstance().context().actionService();
        allFormVersionSyncService = BidanApplication.getInstance().context().allFormVersionSyncService();
        httpAgent = BidanApplication.getInstance().context().getHttpAgent();
        pathAfterFetchListener = new PathAfterFetchListener();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        sendSyncStatusBroadcastMessage(context, FetchStatus.fetchStarted);
        if (BidanApplication.getInstance().context().IsUserLoggedOut()) {
            drishtiLogInfo("Not updating from server as user is not logged in.");
            return;
        }

        FetchStatus fetchStatus = doSync();

        Intent intent = new Intent(context, ZScoreRefreshIntentService.class);
        context.startService(intent);
        if (fetchStatus.equals(FetchStatus.nothingFetched) || fetchStatus.equals(FetchStatus.fetched)) {
            ECSyncUpdater ecSyncUpdater = ECSyncUpdater.getInstance(context);
            ecSyncUpdater.updateLastCheckTimeStamp(Calendar.getInstance().getTimeInMillis());
        }
        pathAfterFetchListener.afterFetch(fetchStatus);
        sendSyncStatusBroadcastMessage(context, fetchStatus);


    }

    private FetchStatus doSync() {
        if (NetworkUtils.isNetworkAvailable()) {
            FetchStatus fetchStatusForForms = sync();
            FetchStatus fetchStatusForActions = actionService.fetchNewActions();
            pathAfterFetchListener.partialFetch(fetchStatusForActions);

            if (BidanApplication.getInstance().context().configuration().shouldSyncForm()) {

                allFormVersionSyncService.verifyFormsInFolder();
                FetchStatus fetchVersionStatus = allFormVersionSyncService.pullFormDefinitionFromServer();
                DownloadStatus downloadStatus = allFormVersionSyncService.downloadAllPendingFormFromServer();

                if (downloadStatus == DownloadStatus.downloaded) {
                    allFormVersionSyncService.unzipAllDownloadedFormFile();
                }

                if (fetchVersionStatus == FetchStatus.fetched || downloadStatus == DownloadStatus.downloaded) {
                    return FetchStatus.fetched;
                }
            }

            return (fetchStatusForForms == FetchStatus.fetched) ? fetchStatusForActions : fetchStatusForForms;

        }

        return FetchStatus.noConnection;
    }


    private FetchStatus sync() {

        try {
            // Fetch locations
            String locations = Utils.getPreference(context, LocationPickerView.PREF_TEAM_LOCATIONS, "");

            if (StringUtils.isBlank(locations)) {
                return FetchStatus.fetchedFailed;
            }

            pushToServer();
            FetchStatus formActionsFetctStatus = pullFormAndActionsFromServer(locations);
            pullStockFromServer();

            return formActionsFetctStatus;
        } catch (Exception e) {
            Log.e(getClass().getName(), "", e);
            return FetchStatus.fetchedFailed;
        }

    }

    private FetchStatus pullFormAndActionsFromServer(String locations) throws Exception {
        int totalCount = 0;
        ECSyncUpdater ecUpdater = ECSyncUpdater.getInstance(context);

        while (true) {
            long startSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
            int eCount = ecUpdater.fetchAllClientsAndEvents(AllConstants.SyncFilters.FILTER_LOCATION_ID, locations);
            totalCount += eCount;
            if (eCount < 0) {
                return FetchStatus.fetchedFailed;
            } else if (eCount == 0) {
                break;
            }

            long lastSyncTimeStamp = ecUpdater.getLastSyncTimeStamp();
            PathClientProcessor.getInstance(context).processClient(ecUpdater.allEvents(startSyncTimeStamp, lastSyncTimeStamp));
            Log.i(getClass().getName(), "Sync count:  " + eCount);
            pathAfterFetchListener.partialFetch(FetchStatus.fetched);
        }


        if (totalCount == 0) {
            return FetchStatus.nothingFetched;
        } else if (totalCount < 0) {
            return FetchStatus.fetchedFailed;
        } else {
            return FetchStatus.fetched;
        }
    }

    private void pushToServer() {
        pushECToServer();
        pushReportsToServer();
        pushStockToServer();

        startSyncValidation();
    }

    private void pushECToServer() {
        EventClientRepository db = BidanApplication.getInstance().eventClientRepository();
        boolean keepSyncing = true;

        while (keepSyncing) {
            try {
                Map<String, Object> pendingEvents = null;
                pendingEvents = db.getUnSyncedEvents(EVENT_FETCH_LIMIT);

                if (pendingEvents.isEmpty()) {
                    return;
                }

                String baseUrl = BidanApplication.getInstance().context().configuration().dristhiBaseURL();
                if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
                }
                // create request body
                JSONObject request = new JSONObject();
                if (pendingEvents.containsKey(context.getString(R.string.clients_key))) {
                    request.put(context.getString(R.string.clients_key), pendingEvents.get(context.getString(R.string.clients_key)));
                }
                if (pendingEvents.containsKey(context.getString(R.string.events_key))) {
                    request.put(context.getString(R.string.events_key), pendingEvents.get(context.getString(R.string.events_key)));
                }
                String jsonPayload = request.toString();
                Response<String> response = httpAgent.post(
                        MessageFormat.format("{0}/{1}",
                                baseUrl,
                                EVENTS_SYNC_PATH),
                        jsonPayload);
                if (response.isFailure()) {
                    Log.e(getClass().getName(), "Events sync failed.");
                    return;
                }
                db.markEventsAsSynced(pendingEvents);
                Log.i(getClass().getName(), "Events synced successfully.");
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }


    }

    private void pullStockFromServer() {
        final String LAST_STOCK_SYNC = "last_stock_sync";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AllSharedPreferences allSharedPreferences = new AllSharedPreferences(preferences);
        String anmId = allSharedPreferences.fetchRegisteredANM();
        String baseUrl = BidanApplication.getInstance().context().configuration().dristhiBaseURL();
        if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
        }

        while (true) {
            long timestamp = preferences.getLong(LAST_STOCK_SYNC, 0);
            String timeStampString = String.valueOf(timestamp);
            String uri = MessageFormat.format("{0}/{1}?providerid={2}&serverVersion={3}",
                    baseUrl,
                    STOCK_SYNC_PATH,
                    anmId,
                    timeStampString
            );
            Response<String> response = httpAgent.fetch(uri);
            if (response.isFailure()) {
                drishtiLogError("Stock pull failed.");
                return;
            }
            String jsonPayload = response.payload();
            ArrayList<Stock> Stock_arrayList = getStockFromPayload(jsonPayload);
            Long highestTimestamp = getHighestTimestampFromStockPayLoad(jsonPayload);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(LAST_STOCK_SYNC, highestTimestamp);
            editor.commit();
            if (Stock_arrayList.isEmpty()) {
                return;
            } else {
                StockRepository stockRepository = BidanApplication.getInstance().stockRepository();
                for (int j = 0; j < Stock_arrayList.size(); j++) {
                    Stock fromServer = Stock_arrayList.get(j);
                    List<Stock> existingStock = stockRepository.findUniqueStock(fromServer.getVaccineTypeId(), fromServer.getTransactionType(), fromServer.getProviderid(),
                            String.valueOf(fromServer.getValue()), String.valueOf(fromServer.getDateCreated()), fromServer.getToFrom());
                    if (!existingStock.isEmpty()) {
                        for (Stock stock : existingStock) {
                            fromServer.setId(stock.getId());
                        }
                    }
                    stockRepository.add(fromServer);
                }

            }
        }
    }

    private Long getHighestTimestampFromStockPayLoad(String jsonPayload) {
        Long toreturn = 0l;
        try {
            JSONObject stockContainer = new JSONObject(jsonPayload);
            if (stockContainer.has(context.getString(R.string.stocks_key))) {
                JSONArray stockArray = stockContainer.getJSONArray(context.getString(R.string.stocks_key));
                for (int i = 0; i < stockArray.length(); i++) {

                    JSONObject stockObject = stockArray.getJSONObject(i);
                    if (stockObject.getLong(context.getString(R.string.server_version_key)) > toreturn) {
                        toreturn = stockObject.getLong(context.getString(R.string.server_version_key));
                    }

                }
            }
        } catch (Exception e) {
            Log.e(getClass().getCanonicalName(), e.getMessage());
        }
        return toreturn;
    }

    private ArrayList<Stock> getStockFromPayload(String jsonPayload) {
        ArrayList<Stock> Stock_arrayList = new ArrayList<>();
        try {
            JSONObject stockcontainer = new JSONObject(jsonPayload);
            if (stockcontainer.has(context.getString(R.string.stocks_key))) {
                JSONArray stockArray = stockcontainer.getJSONArray(context.getString(R.string.stocks_key));
                for (int i = 0; i < stockArray.length(); i++) {
                    JSONObject stockObject = stockArray.getJSONObject(i);
                    Stock stock = new Stock(null,
                            stockObject.getString(context.getString(R.string.transaction_type_key)),
                            stockObject.getString(context.getString(R.string.providerid_key)),
                            stockObject.getInt(context.getString(R.string.value_key)),
                            stockObject.getLong(context.getString(R.string.date_created_key)),
                            stockObject.getString(context.getString(R.string.to_from_key)),
                            BaseRepository.TYPE_Synced,
                            stockObject.getLong(context.getString(R.string.date_updated_key)),
                            stockObject.getString(context.getString(R.string.vaccine_type_id_key)));
                    Stock_arrayList.add(stock);
                }
            }
        } catch (Exception e) {
            Log.e(getClass().getCanonicalName(), e.getMessage());
        }
        return Stock_arrayList;
    }

    private void pushStockToServer() {
        boolean keepSyncing = true;
        int limit = 50;

        try {

            while (keepSyncing) {
                StockRepository stockRepository = BidanApplication.getInstance().stockRepository();
                ArrayList<Stock> stocks = (ArrayList<Stock>) stockRepository.findUnSyncedWithLimit(limit);
                JSONArray stocksarray = createJsonArrayFromStockArray(stocks);
                if (stocks.isEmpty()) {
                    return;
                }

                String baseUrl = BidanApplication.getInstance().context().configuration().dristhiBaseURL();
                if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
                }
                // create request body
                JSONObject request = new JSONObject();
                request.put(context.getString(R.string.stocks_key), stocksarray);

                String jsonPayload = request.toString();
                Response<String> response = httpAgent.post(
                        MessageFormat.format("{0}/{1}",
                                baseUrl,
                                STOCK_Add_PATH),
                        jsonPayload);
                if (response.isFailure()) {
                    Log.e(getClass().getName(), "Stocks sync failed.");
                    return;
                }
                stockRepository.markEventsAsSynced(stocks);
                Log.i(getClass().getName(), "Stocks synced successfully.");
            }
        } catch (JSONException e) {
            Log.e(getClass().getName(), e.getMessage());
        }
    }

    private JSONArray createJsonArrayFromStockArray(ArrayList<Stock> stocks) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < stocks.size(); i++) {
            JSONObject stock = new JSONObject();
            try {
                stock.put("identifier", stocks.get(i).getId());
                stock.put(context.getString(R.string.vaccine_type_id_key), stocks.get(i).getVaccineTypeId());
                stock.put(context.getString(R.string.transaction_type_key), stocks.get(i).getTransactionType());
                stock.put(context.getString(R.string.providerid_key), stocks.get(i).getProviderid());
                stock.put(context.getString(R.string.date_created_key), stocks.get(i).getDateCreated());
                stock.put(context.getString(R.string.value_key), stocks.get(i).getValue());
                stock.put(context.getString(R.string.to_from_key), stocks.get(i).getToFrom());
                stock.put(context.getString(R.string.date_updated_key), stocks.get(i).getUpdatedAt());
                array.put(stock);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    private void pushReportsToServer() {
        EventClientRepository db = BidanApplication.getInstance().eventClientRepository();
        try {
            boolean keepSyncing = true;
            int limit = 50;
            while (keepSyncing) {
                List<JSONObject> pendingReports = null;
                pendingReports = db.getUnSyncedReports(limit);

                if (pendingReports.isEmpty()) {
                    return;
                }

                String baseUrl = BidanApplication.getInstance().context().configuration().dristhiBaseURL();
                if (baseUrl.endsWith(context.getString(R.string.url_separator))) {
                    baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(context.getString(R.string.url_separator)));
                }
                // create request body
                JSONObject request = new JSONObject();

                request.put("reports", pendingReports);
                String jsonPayload = request.toString();
                Response<String> response = httpAgent.post(
                        MessageFormat.format("{0}/{1}",
                                baseUrl,
                                REPORTS_SYNC_PATH),
                        jsonPayload);
                if (response.isFailure()) {
                    Log.e(getClass().getName(), "Reports sync failed.");
                    return;
                }
                db.markReportsAsSynced(pendingReports);
                Log.i(getClass().getName(), "Reports synced successfully.");
            }
        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage());
        }
    }

    private void sendSyncStatusBroadcastMessage(Context context, FetchStatus fetchStatus) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        context.sendBroadcast(intent);
    }

    private void drishtiLogInfo(String message) {
        org.smartregister.util.Log.logInfo(message);
    }

    private void drishtiLogError(String message) {
        org.smartregister.util.Log.logError(message);
    }

    private void startSyncValidation() {
        Intent intent = new Intent(context, ValidateIntentService.class);
        startService(intent);
    }

}
