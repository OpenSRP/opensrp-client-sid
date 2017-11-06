package org.smartregister.bidan.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartregister.bidan.application.BidanApplication;
import org.smartregister.bidan.repository.StockRepository;
import org.smartregister.bidan.tabfragments.CurrentStock;
import org.smartregister.bidan.tabfragments.PlanningStockFragment;
import org.smartregister.immunization.domain.VaccineType;
import org.smartregister.bidan.R;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.view.activity.DrishtiApplication;

public class StockControlActivity extends AppCompatActivity {

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public VaccineType vaccineType;
    public PlanningStockFragment planningStockFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_control);
        vaccineType = (VaccineType) getIntent().getSerializableExtra("vaccine_type");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        toolbar.setTitle("");
        setTitle("");

        ((TextView) toolbar.findViewById(R.id.title)).setText("Stock Control > " + vaccineType.getName());


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout.setupWithViewPager(mViewPager);
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

        AllSharedPreferences allSharedPreferences = BidanApplication.getInstance().context().allSharedPreferences();
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

        initializeCustomNavbarLIsteners();

    }

    private void initializeCustomNavbarLIsteners() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Button logoutButton = (Button) navigationView.findViewById(R.id.logout_b);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrishtiApplication application = (DrishtiApplication) getApplication();
                application.logoutCurrentUser();
                finish();
            }
        });

        ImageButton cancelButton = (ImageButton) navigationView.findViewById(R.id.cancel_b);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        TextView initialsTV = (TextView) navigationView.findViewById(R.id.initials_tv);
        String preferredName = BidanApplication.getInstance().context().allSharedPreferences().getANMPreferredName(
                BidanApplication.getInstance().context().allSharedPreferences().fetchRegisteredANM());
        if (!TextUtils.isEmpty(preferredName)) {
            String[] initialsArray = preferredName.split(" ");
            String initials = "";
            if (initialsArray.length > 0) {
                initials = initialsArray[0].substring(0, 1);
                if (initialsArray.length > 1) {
                    initials = initials + initialsArray[1].substring(0, 1);
                }
            }

            initialsTV.setText(initials.toUpperCase());
        }

        TextView nameTV = (TextView) navigationView.findViewById(R.id.name_tv);
        nameTV.setText(preferredName);


        LinearLayout syncMenuItem = (LinearLayout) drawer.findViewById(R.id.nav_sync);
        syncMenuItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startSync();
                drawer.closeDrawer(GravityCompat.START);
            }
        });
        LinearLayout addchild = (LinearLayout) drawer.findViewById(R.id.nav_register);
        addchild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startJsonForm("child_enrollment", null);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout outofcatchment = (LinearLayout) drawer.findViewById(R.id.nav_record_vaccination_out_catchment);
        outofcatchment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startJsonForm("out_of_catchment_service", null);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout stockregister = (LinearLayout) drawer.findViewById(R.id.stockcontrol);
        stockregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StockActivity.class);
                startActivity(intent);
                drawer.closeDrawer(GravityCompat.START);

            }
        });
        LinearLayout childregister = (LinearLayout) drawer.findViewById(R.id.child_register);
        childregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BidanApplication.setCrashlyticsUser(BidanApplication.getInstance().context());
                Intent intent = new Intent(getApplicationContext(), ChildSmartRegisterActivity.class);
                intent.putExtra(BaseRegisterActivity.IS_REMOTE_LOGIN, false);
                startActivity(intent);
                finish();
                drawer.closeDrawer(GravityCompat.START);

//                finish();
            }
        });
        stockregister.setBackgroundColor(getResources().getColor(R.color.tintcolor));

    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
////        getMenuInflater().inflate(R.menu.menu_stock_control, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    public int getcurrentVialNumber() {
        net.sqlcipher.database.SQLiteDatabase db = BidanApplication.getInstance().getRepository().getReadableDatabase();
        Cursor c = db.rawQuery("Select sum(value) from Stocks where " + StockRepository.DATE_CREATED + " <= " + new DateTime(System.currentTimeMillis()).toDate().getTime() + " and " + StockRepository.VACCINE_TYPE_ID + " = " + vaccineType.getId(), null);
        String stockvalue = "0";
        if (c.getCount() > 0) {
            c.moveToFirst();
            if (c.getString(0) != null && !StringUtils.isBlank(c.getString(0))) {
                stockvalue = c.getString(0);
            }
            c.close();
        } else {
            c.close();
        }
        return Integer.parseInt(stockvalue);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_stock_control, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return CurrentStock.newInstance("", "");
                case 1:
                    planningStockFragment = PlanningStockFragment.newInstance("", "");
                    return planningStockFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Current Stock";
                case 1:
                    return "Stock Planning";
            }
            return null;
        }
    }
}
