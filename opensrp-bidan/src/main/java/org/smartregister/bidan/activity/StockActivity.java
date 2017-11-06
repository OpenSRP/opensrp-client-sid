package org.smartregister.bidan.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.smartregister.bidan.application.BidanApplication;
import org.smartregister.bidan.repository.StockRepository;
import org.smartregister.bidan.toolbar.LocationSwitcherToolbar;
import org.smartregister.immunization.domain.VaccineType;
import org.smartregister.bidan.R;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;

/**
 * Created by raihan on 5/23/17.
 */
public class StockActivity extends BaseActivity {
    private GridView stockGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());


        LocationSwitcherToolbar toolbar = (LocationSwitcherToolbar) getToolbar();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StockActivity.this, ChildSmartRegisterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        DrawerLayout drawer = (DrawerLayout) findViewById(getDrawerLayoutId());
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        int toolbarResource = R.drawable.vertical_separator_male;
        toolbar.updateSeparatorView(toolbarResource);
        toolbar.init(this);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Stock Control");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView nameInitials = (TextView) findViewById(R.id.name_inits);
        nameInitials.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (!drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });


        AllSharedPreferences allSharedPreferences = getOpenSRPContext().allSharedPreferences();
        String preferredName = allSharedPreferences.getANMPreferredName(allSharedPreferences.fetchRegisteredANM());
        if (!preferredName.isEmpty()) {
            String[] preferredNameArray = preferredName.split(" ");
            String initials = "";
            if (preferredNameArray.length > 1) {
                initials = String.valueOf(preferredNameArray[0].charAt(0)) + String.valueOf(preferredNameArray[1].charAt(0));
            } else if (preferredNameArray.length == 1) {
                initials = String.valueOf(preferredNameArray[0].charAt(0));
            }
            nameInitials.setText(initials);
        }

//        toolbar.setOnLocationChangeListener(this);
//


        stockGrid = (GridView) findViewById(R.id.stockgrid);
    }

    @SuppressWarnings("unchecked")
    private void refreshadapter() {
        ArrayList<VaccineType> allVaccineTypes = (ArrayList) BidanApplication.getInstance().vaccineTypeRepository().getAllVaccineTypes(null);
        VaccineType[] allVaccineTypesarray = allVaccineTypes.toArray(new VaccineType[allVaccineTypes.size()]);
        stockGridAdapter adapter = new stockGridAdapter(this, allVaccineTypesarray);
        stockGrid.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshadapter();
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        LinearLayout stockregister = (LinearLayout) drawer.findViewById(R.id.stockcontrol);
        stockregister.setBackgroundColor(getResources().getColor(R.color.tintcolor));
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_stock;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_register) {
////            startFormActivity("child_enrollment", null, null);
//        } else if (id == R.id.nav_record_vaccination_out_catchment) {
////            startFormActivity("out_of_catchment_service", null, null);
//        } else if (id == R.id.stock) {
//            Intent intent = new Intent(this, StockActivity.class);
//            startActivity(intent);
//        } else if (id == R.id.nav_sync) {
////            startSync();
//        }
//
//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected int getDrawerLayoutId() {
        return R.id.drawer_layout;
    }

    @Override
    protected int getToolbarId() {
        return LocationSwitcherToolbar.TOOLBAR_ID;
    }

    @Override
    protected Class onBackActivity() {
        return ChildSmartRegisterActivity.class;
    }

    class stockGridAdapter extends BaseAdapter {
        private final Context context;
        private final VaccineType[] vaccineTypes;

        public stockGridAdapter(Context context, VaccineType[] vaccineTypes) {
            this.context = context;
            this.vaccineTypes = vaccineTypes;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View gridView;

            if (convertView == null) {

                gridView = new View(context);

                // get layout from mobile.xml
                gridView = inflater.inflate(R.layout.stock_grid_block, null);

                // set value into textview
                TextView name = (TextView) gridView
                        .findViewById(R.id.vaccine_type_name);
                TextView doses = (TextView) gridView
                        .findViewById(R.id.doses);
                TextView vials = (TextView) gridView
                        .findViewById(R.id.vials);

                // set image based on selected text


                final VaccineType vaccineType = vaccineTypes[position];
                StockRepository stockRepository = BidanApplication.getInstance().stockRepository();
                int currentvials = stockRepository.getBalanceFromNameAndDate(vaccineType.getName(), System.currentTimeMillis());
                name.setText(vaccineType.getName());

                doses.setText("" + currentvials * vaccineType.getDoses() + " doses");

                vials.setText("" + currentvials + " vials");

                gridView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(StockActivity.this, StockControlActivity.class);
                        intent.putExtra("vaccine_type", vaccineType);
                        startActivity(intent);
                    }
                });

            } else {
                gridView = convertView;
            }

            return gridView;
        }

        @Override
        public int getCount() {
            return vaccineTypes.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }
}
