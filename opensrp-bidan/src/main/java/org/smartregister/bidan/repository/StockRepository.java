package org.smartregister.bidan.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.immunization.domain.VaccineType;
import org.smartregister.immunization.repository.VaccineTypeRepository;
import org.smartregister.bidan.application.BidanApplication;
import org.smartregister.bidan.domain.Stock;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by raihan@mpower-social.com on 18-May-17.
 */

public class StockRepository extends BaseRepository {
    private static final String TAG = StockRepository.class.getCanonicalName();
    private static final String stock_SQL = "CREATE TABLE Stocks (_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
            "vaccine_type_id VARCHAR NOT NULL," +
            "transaction_type VARCHAR NULL," +
            "providerid VARCHAR NOT NULL," +
            "value INTEGER," +
            "date_created DATETIME NOT NULL," +
            "to_from VARCHAR NULL," +
            "sync_status VARCHAR," +
            "date_updated INTEGER NULL)";
    public static final String stock_TABLE_NAME = "Stocks";
    private static final String ID_COLUMN = "_id";
    public static final String VACCINE_TYPE_ID = "vaccine_type_id";
    public static final String TRANSACTION_TYPE = "transaction_type";
    private static final String PROVIDER_ID = "providerid";
    private static final String VALUE = "value";
    public static final String DATE_CREATED = "date_created";
    private static final String TO_FROM = "to_from";
    private static final String SYNC_STATUS = "sync_status";
    public static final String DATE_UPDATED = "date_updated";
    public static final String[] stock_TABLE_COLUMNS = {ID_COLUMN, VACCINE_TYPE_ID, TRANSACTION_TYPE, PROVIDER_ID, VALUE, DATE_CREATED, TO_FROM, SYNC_STATUS, DATE_UPDATED};

    public static final String TYPE_Unsynced = "Unsynced";
    private static final String TYPE_Synced = "Synced";

    public StockRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(stock_SQL);
    }

    public void add(Stock stock) {
        if (stock == null) {
            return;
        }
        if (StringUtils.isBlank(stock.getSyncStatus())) {
            stock.setSyncStatus(TYPE_Unsynced);
        }

        if (stock.getUpdatedAt() == null) {
            stock.setUpdatedAt(Calendar.getInstance().getTimeInMillis());
        }

        SQLiteDatabase database = getWritableDatabase();
        if (stock.getId() == null) {
            stock.setId(database.insert(stock_TABLE_NAME, null, createValuesFor(stock)));
        } else {
            //mark the vaccine as unsynced for processing as an updated stock
            String idSelection = ID_COLUMN + " = ?";
            database.update(stock_TABLE_NAME, createValuesFor(stock), idSelection, new String[]{stock.getId().toString()});
        }
//        updateFtsSearch(vaccine);
    }

    private ContentValues createValuesFor(Stock stock) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, stock.getId());
        values.put(VACCINE_TYPE_ID, stock.getVaccineTypeId());
        values.put(TRANSACTION_TYPE, stock.getTransactionType());
        values.put(PROVIDER_ID, stock.getProviderid());
        values.put(VALUE, stock.getValue());
        values.put(DATE_CREATED, stock.getDateCreated() != null ? stock.getDateCreated() : null);
        values.put(TO_FROM, stock.getToFrom());
        values.put(SYNC_STATUS, stock.getSyncStatus());
        values.put(DATE_UPDATED, stock.getUpdatedAt() != null ? stock.getUpdatedAt() : null);
        return values;
    }

    public List<Stock> findUnSyncedBeforeTime(int hours) {
        List<Stock> stocks = new ArrayList<>();
        Cursor cursor = null;
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -hours);

            Long time = calendar.getTimeInMillis();

            cursor = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, DATE_UPDATED + " < ? AND " + SYNC_STATUS + " = ?", new String[]{time.toString(), TYPE_Unsynced}, null, null, null, null);
            stocks = readAllstocks(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return stocks;
    }

    public List<Stock> findUnSyncedWithLimit(int limit) {
        List<Stock> stocks = new ArrayList<>();
        Cursor cursor = null;
        try {


            cursor = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, SYNC_STATUS + " = ?", new String[]{TYPE_Unsynced}, null, null, null, "" + limit);
            stocks = readAllstocks(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return stocks;
    }

    public List<Stock> findUniqueStock(String vaccine_type_id, String transaction_type, String providerid, String value, String date_created, String to_from) {
        List<Stock> stocks = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, VACCINE_TYPE_ID + " = ? AND " + TRANSACTION_TYPE + " = ? AND " + PROVIDER_ID + " = ? AND " + VALUE + " = ? AND " + DATE_CREATED + " = ? AND " + TO_FROM + " = ?", new String[]{vaccine_type_id, transaction_type, providerid, value, date_created, to_from}, null, null, null, null);
            stocks = readAllstocks(cursor);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return stocks;
    }

    public Stock readAllStockforCursorAdapter(Cursor cursor) {


        return new Stock(cursor.getLong(cursor.getColumnIndex(ID_COLUMN)),
                cursor.getString(cursor.getColumnIndex(TRANSACTION_TYPE)),
                cursor.getString(cursor.getColumnIndex(PROVIDER_ID)),
                cursor.getInt(cursor.getColumnIndex(VALUE)),
                cursor.getLong(cursor.getColumnIndex(DATE_CREATED)),
                cursor.getString(cursor.getColumnIndex(TO_FROM)),
                cursor.getString(cursor.getColumnIndex(SYNC_STATUS)),
                cursor.getLong(cursor.getColumnIndex(DATE_UPDATED)),
                cursor.getString(cursor.getColumnIndex(VACCINE_TYPE_ID)));
    }


//    public List<Vaccine> findByEntityId(String entityId) {
//        SQLiteDatabase database = getReadableDatabase();
//        Cursor cursor = database.query(VACCINE_TABLE_NAME, VACCINE_TABLE_COLUMNS, BASE_ENTITY_ID + " = ? ORDER BY " + UPDATED_AT_COLUMN, new String[]{entityId}, null, null, null, null);
//        return readAllVaccines(cursor);
//    }

//    public Vaccine find(Long caseId) {
//        Vaccine vaccine = null;
//        Cursor cursor = null;
//        try {
//            cursor = getReadableDatabase().query(VACCINE_TABLE_NAME, VACCINE_TABLE_COLUMNS, ID_COLUMN + " = ?", new String[]{caseId.toString()}, null, null, null, null);
//            List<Vaccine> vaccines = readAllVaccines(cursor);
//            if (!vaccines.isEmpty()) {
//                vaccine = vaccines.get(0);
//            }
//        } catch (Exception e) {
//            Log.e(TAG, e.getMessage(), e);
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return vaccine;
//    }

//    public void deleteVaccine(Long caseId) {
//        Vaccine vaccine = find(caseId);
//        if(vaccine != null) {
//            getWritableDatabase().delete(VACCINE_TABLE_NAME, ID_COLUMN + "= ?", new String[]{caseId.toString()});
//
//            updateFtsSearch(vaccine.getBaseEntityId(), vaccine.getName());
//        }
//    }

//    public void close(Long caseId) {
//        ContentValues values = new ContentValues();
//        values.put(SYNC_STATUS, TYPE_Synced);
//        getWritableDatabase().update(VACCINE_TABLE_NAME, values, ID_COLUMN + " = ?", new String[]{caseId.toString()});
//    }

    private List<Stock> readAllstocks(Cursor cursor) {
        List<Stock> stocks = new ArrayList<>();


        try {

            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {

                    stocks.add(
                            new Stock(cursor.getLong(cursor.getColumnIndex(ID_COLUMN)),
                                    cursor.getString(cursor.getColumnIndex(TRANSACTION_TYPE)),
                                    cursor.getString(cursor.getColumnIndex(PROVIDER_ID)),
                                    cursor.getInt(cursor.getColumnIndex(VALUE)),
                                    cursor.getLong(cursor.getColumnIndex(DATE_CREATED)),
                                    cursor.getString(cursor.getColumnIndex(TO_FROM)),
                                    cursor.getString(cursor.getColumnIndex(SYNC_STATUS)),
                                    cursor.getLong(cursor.getColumnIndex(DATE_UPDATED)),
                                    cursor.getString(cursor.getColumnIndex(VACCINE_TYPE_ID))
                            ));

                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.e(getClass().getCanonicalName(), e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return stocks;
    }

    public int getVaccineUsedToday(Long date, String vaccineName) {
        int vaccineUsed = 0;
        DateTime thedate = new DateTime(date);
        DateTime startofday = thedate.withTimeAtStartOfDay();
        DateTime endofday = thedate.plusDays(1).withTimeAtStartOfDay();
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.rawQuery("Select count(*) from vaccines where date >= " + startofday.getMillis() + " and date < " + endofday.getMillis() + " and name like '%" + vaccineName + "%'", null);
        c.moveToFirst();
        if (c.getCount() > 0 && !StringUtils.isBlank(c.getString(0))) {
            vaccineUsed = Integer.parseInt(c.getString(0));
        }

        c.close();
        return vaccineUsed;
    }

    public int getVaccineUsedUntildate(Long date, String vaccineName) {
        int vaccineUsed = 0;
        DateTime thedate = new DateTime(date);
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.rawQuery("Select count(*) from vaccines where date <= " + thedate.getMillis() + " and name like '%" + vaccineName + "%'", null);
        c.moveToFirst();
        if (c.getCount() > 0 && !StringUtils.isBlank(c.getString(0))) {
            vaccineUsed = Integer.parseInt(c.getString(0));
        }

        c.close();
        return vaccineUsed;
    }


    public int getBalanceBefore(Stock stock) {
        SQLiteDatabase database = getReadableDatabase();
//        Cursor c = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, DATE_UPDATED + " < ?", new String[]{""+updatedAt.longValue()}, null, null, null, null);
        Cursor c = database.rawQuery("Select sum(value) from Stocks Where date_updated <" + stock.getUpdatedAt() + " and date_created <=" + new DateTime(stock.getDateCreated()).toDate().getTime() + " and " + VACCINE_TYPE_ID + " = " + stock.getVaccineTypeId(), null);
        if (c.getCount() == 0) {
            c.close();
            return 0;
        } else {
            c.moveToFirst();
            if (c.getString(0) != null) {
                int toreturn = Integer.parseInt(c.getString(0));
                c.close();
                return toreturn;
            } else {
                c.close();
                return 0;
            }
        }
    }

    public int getBalanceBeforeCheck(Stock stock) {
        int sum = 0;
        SQLiteDatabase database = getReadableDatabase();

        Cursor c = database.rawQuery("Select sum(value) from Stocks Where date_created = " + stock.getDateCreated() + " and date_updated <" + stock.getUpdatedAt() + " and " + VACCINE_TYPE_ID + " = " + stock.getVaccineTypeId(), null);

//        Cursor c = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, DATE_UPDATED + " < ?", new String[]{""+updatedAt.longValue()}, null, null, null, null);
//      c =database.rawQuery("Select sum(value) from Stocks Where date_updated <" +stock.getUpdatedAt()+ " and date_created <=" +new DateTime(stock.getDate_created()).toDate().getTime()+ " and "+VACCINE_TYPE_ID+ " = "+stock.getVaccine_type_id(),null);
        if (c.getCount() == 0) {
            sum = 0;
        } else {
            c.moveToFirst();
            if (c.getString(0) != null) {
                sum = Integer.parseInt(c.getString(0));
            } else {
                sum = 0;
            }
        }
        c.close();
        c = database.rawQuery("Select sum(value) from Stocks Where date_created <" + stock.getDateCreated() + " and " + VACCINE_TYPE_ID + " = " + stock.getVaccineTypeId(), null);
        if (c.getCount() == 0) {
            sum = sum + 0;
        } else {
            c.moveToFirst();
            if (c.getString(0) != null) {
                sum = sum + Integer.parseInt(c.getString(0));
            } else {
                sum = sum + 0;
            }
        }
        c.close();
        return sum;
    }

    @SuppressWarnings("unchecked")
    public int getBalanceFromNameAndDate(String Name, Long updatedat) {
        SQLiteDatabase database = getReadableDatabase();
//      Cursor c = getReadableDatabase().query(stock_TABLE_NAME, stock_TABLE_COLUMNS, DATE_UPDATED + " < ?", new String[]{""+updatedAt.longValue()}, null, null, null, null);
        VaccineTypeRepository vtr = BidanApplication.getInstance().vaccineTypeRepository();
        ArrayList<VaccineType> allvaccinetypes = (ArrayList) vtr.findIDByName(Name);
        String id_for_vaccine = "";
        if (allvaccinetypes.size() > 0) {
            id_for_vaccine = "" + allvaccinetypes.get(0).getId();
        }
        Cursor c = database.rawQuery("Select sum(value) from Stocks Where date_created <=" + updatedat + " and " + VACCINE_TYPE_ID + " = " + id_for_vaccine, null);
        if (c.getCount() == 0) {
            c.close();
            return 0;
        } else {
            c.moveToFirst();
            if (c.getString(0) != null) {
                int toreturn = Integer.parseInt(c.getString(0));
                c.close();
                return toreturn;
            } else {
                c.close();
                return 0;
            }
        }
    }

    public void markEventsAsSynced(ArrayList<Stock> stocks) {
        for (int i = 0; i < stocks.size(); i++) {
            Stock stockToAdd = stocks.get(i);
            stockToAdd.setSyncStatus(TYPE_Synced);
            add(stockToAdd);
        }
    }
}
