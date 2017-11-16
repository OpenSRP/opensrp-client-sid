package org.smartregister.bidan_cloudant.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

import org.smartregister.Context;
import org.smartregister.bidan_cloudant.R;
import org.smartregister.bidan_cloudant.libs.FlurryFacade;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.repository.DetailsRepository;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import util.Utils;

import static org.smartregister.util.StringUtil.humanize;

//import org.smartregister.bidan_cloudant.face.camera.SmartShutterActivity;

/**
 * Created by Iq on 09/06/16.
 */
public class KMotherDetailActivity extends Activity {
    private static final String TAG = KMotherDetailActivity.class.getSimpleName();
    SimpleDateFormat timer = new SimpleDateFormat("hh:mm:ss", Locale.US);

    //image retrieving
    public static CommonPersonObjectClient kiclient;

    ImageView kiview;
    private static HashMap<String, String> hash;
    private boolean updateMode = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mother_detail_activity);

        String DetailStart = timer.format(new Date());
        Map<String, String> Detail = new HashMap<>();
        Detail.put("start", DetailStart);
        FlurryAgent.logEvent("KI_detail_view",Detail, true );

        kiview = (ImageView)findViewById(R.id.motherdetailprofileview);

        //profile
        TextView nama = (TextView) findViewById(R.id.txt_wife_name);
        TextView nik = (TextView) findViewById(R.id.txt_nik);
        TextView husband_name = (TextView) findViewById(R.id.txt_husband_name);
        TextView dob = (TextView) findViewById(R.id.txt_dob);
        TextView phone = (TextView) findViewById(R.id.txt_contact_phone_number);
        TextView risk1 = (TextView) findViewById(R.id.txt_risk1);
        TextView risk2 = (TextView) findViewById(R.id.txt_risk2);
        TextView risk3 = (TextView) findViewById(R.id.txt_risk3);
        TextView risk4 = (TextView) findViewById(R.id.txt_risk4);

        final TextView show_risk = (TextView) findViewById(R.id.show_more);
        final TextView show_detail = (TextView) findViewById(R.id.show_more_detail);

        //detail data
        TextView village = (TextView) findViewById(R.id.txt_village_name);
        TextView subvillage = (TextView) findViewById(R.id.txt_subvillage);
        TextView age = (TextView) findViewById(R.id.txt_age);
        TextView alamat = (TextView) findViewById(R.id.txt_alamat);
        TextView education = (TextView) findViewById(R.id.txt_edu);
        TextView religion = (TextView) findViewById(R.id.txt_agama);
        TextView job = (TextView) findViewById(R.id.txt_job);
        TextView gakin = (TextView) findViewById(R.id.txt_gakin);
        TextView blood_type = (TextView) findViewById(R.id.txt_blood);
        TextView asuransi = (TextView) findViewById(R.id.txt_asuransi);

        //detail RISK
        TextView highRiskSTIBBVs = (TextView) findViewById(R.id.txt_highRiskSTIBBVs);
        TextView highRiskEctopicPregnancy = (TextView) findViewById(R.id.txt_highRiskEctopicPregnancy);
        TextView highRiskCardiovascularDiseaseRecord = (TextView) findViewById(R.id.txt_highRiskCardiovascularDiseaseRecord);
        TextView highRiskDidneyDisorder = (TextView) findViewById(R.id.txt_highRiskDidneyDisorder);
        TextView highRiskHeartDisorder = (TextView) findViewById(R.id.txt_highRiskHeartDisorder);
        TextView highRiskAsthma = (TextView) findViewById(R.id.txt_highRiskAsthma);
        TextView highRiskTuberculosis = (TextView) findViewById(R.id.txt_highRiskTuberculosis);
        TextView highRiskMalaria = (TextView) findViewById(R.id.txt_highRiskMalaria);
        TextView highRiskPregnancyPIH = (TextView) findViewById(R.id.txt_highRiskPregnancyPIH);
        TextView highRiskPregnancyProteinEnergyMalnutrition = (TextView) findViewById(R.id.txt_highRiskPregnancyProteinEnergyMalnutrition);
        TextView txt_highRiskLabourTBRisk = (TextView) findViewById(R.id.txt_highRiskLabourTBRisk);
        TextView txt_HighRiskLabourSectionCesareaRecord = (TextView) findViewById(R.id.txt_HighRiskLabourSectionCesareaRecord);
        TextView txt_highRisklabourFetusNumber = (TextView) findViewById(R.id.txt_highRisklabourFetusNumber);
        TextView txt_highRiskLabourFetusSize = (TextView) findViewById(R.id.txt_highRiskLabourFetusSize);
        TextView txt_lbl_highRiskLabourFetusMalpresentation = (TextView) findViewById(R.id.txt_lbl_highRiskLabourFetusMalpresentation);
        TextView txt_highRiskPregnancyAnemia = (TextView) findViewById(R.id.txt_highRiskPregnancyAnemia);
        TextView txt_highRiskPregnancyDiabetes = (TextView) findViewById(R.id.txt_highRiskPregnancyDiabetes);
        TextView HighRiskPregnancyTooManyChildren = (TextView) findViewById(R.id.txt_HighRiskPregnancyTooManyChildren);
        TextView highRiskPostPartumSectioCaesaria = (TextView) findViewById(R.id.txt_highRiskPostPartumSectioCaesaria);
        TextView highRiskPostPartumForceps = (TextView) findViewById(R.id.txt_highRiskPostPartumForceps);
        TextView highRiskPostPartumVacum = (TextView) findViewById(R.id.txt_highRiskPostPartumVacum);
        TextView highRiskPostPartumPreEclampsiaEclampsia = (TextView) findViewById(R.id.txt_highRiskPostPartumPreEclampsiaEclampsia);
        TextView highRiskPostPartumMaternalSepsis = (TextView) findViewById(R.id.txt_highRiskPostPartumMaternalSepsis);
        TextView highRiskPostPartumInfection = (TextView) findViewById(R.id.txt_highRiskPostPartumInfection);
        TextView highRiskPostPartumHemorrhage = (TextView) findViewById(R.id.txt_highRiskPostPartumHemorrhage);
        TextView highRiskPostPartumPIH = (TextView) findViewById(R.id.txt_highRiskPostPartumPIH);
        TextView highRiskPostPartumDistosia = (TextView) findViewById(R.id.txt_highRiskPostPartumDistosia);
        TextView txt_highRiskHIVAIDS = (TextView) findViewById(R.id.txt_highRiskHIVAIDS);

        ImageButton back = (ImageButton) findViewById(R.id.btn_back_to_home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(KMotherDetailActivity.this, KMotherSmartRegisterActivity.class));
                overridePendingTransition(0, 0);

                String DetailEnd = timer.format(new Date());
                Map<String, String> Detail = new HashMap<>();
                Detail.put("end", DetailEnd);
                FlurryAgent.logEvent("KI_detail_view", Detail, true);
            }
        });

        DetailsRepository detailsRepository = Context.getInstance().detailsRepository();
        detailsRepository.updateDetails(kiclient);

        //set profile image by passing the client id.If the image doesn't exist in the image repository then download and save locally
//        DrishtiApplication.getCachedImageLoaderInstance().getImageByClientId(kiclient.getCaseId(), OpenSRPImageLoader.getStaticImageListener(kiview, R.mipmap.woman_placeholder, R.mipmap.woman_placeholder));

        Utils.setImagetoHolderFromUri(this,
                DrishtiApplication.getAppDir() + File.separator + kiclient.getDetails().get("base_entity_id") + ".JPEG",
                kiview, R.mipmap.woman_placeholder);

        nama.setText(String.format("%s%s", getResources().getString(R.string.name), kiclient.getColumnmaps().get("namalengkap") != null ? kiclient.getColumnmaps().get("namalengkap") : "-"));
        nik.setText(String.format("%s%s", getResources().getString(R.string.nik), kiclient.getDetails().get("nik") != null ? kiclient.getDetails().get("nik") : "-"));
        husband_name.setText(String.format("%s%s", getResources().getString(R.string.husband_name), kiclient.getColumnmaps().get("namaSuami") != null ? kiclient.getColumnmaps().get("namaSuami") : "-"));
        String tgl = kiclient.getDetails().get("tanggalLahir") != null ? kiclient.getDetails().get("tanggalLahir") : "-";
        String tgl_lahir = tgl.substring(0, tgl.indexOf("T"));
        dob.setText(String.format("%s%s", getResources().getString(R.string.dob), tgl_lahir));
        phone.setText(String.format("No HP: %s", kiclient.getDetails().get("NomorTelponHp") != null ? kiclient.getDetails().get("NomorTelponHp") : "-"));

        //risk
        if(kiclient.getDetails().get("highRiskPregnancyYoungMaternalAge") != null ){
            risk1.setText(String.format("%s%s", getResources().getString(R.string.highRiskPregnancyYoungMaternalAge), humanize(kiclient.getDetails().get("highRiskPregnancyYoungMaternalAge"))));
        }
        if(kiclient.getDetails().get("highRiskPregnancyOldMaternalAge") != null ){
            risk1.setText(String.format("%s%s", getResources().getString(R.string.highRiskPregnancyOldMaternalAge), humanize(kiclient.getDetails().get("highRiskPregnancyYoungMaternalAge"))));
        }
        if(kiclient.getDetails().get("highRiskPregnancyProteinEnergyMalnutrition") != null
                || kiclient.getDetails().get("HighRiskPregnancyAbortus") != null
                || kiclient.getDetails().get("HighRiskLabourSectionCesareaRecord" ) != null
                ){
            risk2.setText(String.format("%s%s", getResources().getString(R.string.highRiskPregnancyProteinEnergyMalnutrition), humanize(kiclient.getDetails().get("highRiskPregnancyProteinEnergyMalnutrition"))));
            risk3.setText(String.format("%s%s", getResources().getString(R.string.HighRiskPregnancyAbortus), humanize(kiclient.getDetails().get("HighRiskPregnancyAbortus"))));
            risk4.setText(String.format("%s%s", getResources().getString(R.string.HighRiskLabourSectionCesareaRecord), humanize(kiclient.getDetails().get("HighRiskLabourSectionCesareaRecord"))));
        }

        show_risk.setText(getResources().getString(R.string.show_more_button));
        show_detail.setText(getResources().getString(R.string.show_less_button));

        //detail
        village.setText(String.format(": %s", humanize(kiclient.getDetails().get("cityVillage") != null ? kiclient.getDetails().get("cityVillage") : "-")));
        subvillage.setText(String.format(": %s", humanize(kiclient.getDetails().get("address1") != null ? kiclient.getDetails().get("address1") : "-")));
        age.setText(String.format(": %s", humanize(kiclient.getColumnmaps().get("umur") != null ? kiclient.getColumnmaps().get("umur") : "-")));
        alamat.setText(String.format(": %s", humanize(kiclient.getDetails().get("address3") != null ? kiclient.getDetails().get("address3") : "-")));
        education.setText(String.format(": %s", humanize(kiclient.getDetails().get("pendidikan") != null ? kiclient.getDetails().get("pendidikan") : "-")));
        religion.setText(String.format(": %s", humanize(kiclient.getDetails().get("agama") != null ? kiclient.getDetails().get("agama") : "-")));
        job.setText(String.format(": %s", humanize(kiclient.getDetails().get("pekerjaan") != null ? kiclient.getDetails().get("pekerjaan") : "-")));
        gakin.setText(String.format(": %s", humanize(kiclient.getDetails().get("gakinTidak") != null ? kiclient.getDetails().get("gakinTidak") : "-")));
        blood_type.setText(String.format(": %s", humanize(kiclient.getDetails().get("golonganDarah") != null ? kiclient.getDetails().get("golonganDarah") : "-")));
        asuransi.setText(String.format(": %s", humanize(kiclient.getDetails().get("asuransiJiwa") != null ? kiclient.getDetails().get("asuransiJiwa") : "-")));

        //risk detail
        highRiskSTIBBVs.setText(humanize(kiclient.getDetails().get("highRiskSTIBBVs") != null ? kiclient.getDetails().get("highRiskSTIBBVs") : "-"));
        highRiskEctopicPregnancy.setText(humanize (kiclient.getDetails().get("highRiskEctopicPregnancy") != null ? kiclient.getDetails().get("highRiskEctopicPregnancy") : "-"));
        highRiskCardiovascularDiseaseRecord.setText(humanize(kiclient.getDetails().get("highRiskCardiovascularDiseaseRecord") != null ? kiclient.getDetails().get("highRiskCardiovascularDiseaseRecord") : "-"));
        highRiskDidneyDisorder.setText(humanize(kiclient.getDetails().get("highRiskDidneyDisorder") != null ? kiclient.getDetails().get("highRiskDidneyDisorder") : "-"));
        highRiskHeartDisorder.setText(humanize(kiclient.getDetails().get("highRiskHeartDisorder") != null ? kiclient.getDetails().get("highRiskHeartDisorder") : "-"));
        highRiskAsthma.setText(humanize(kiclient.getDetails().get("highRiskAsthma") != null ? kiclient.getDetails().get("highRiskAsthma") : "-"));
        highRiskTuberculosis.setText(humanize(kiclient.getDetails().get("highRiskTuberculosis") != null ? kiclient.getDetails().get("highRiskTuberculosis") : "-"));
        highRiskMalaria.setText(humanize(kiclient.getDetails().get("highRiskMalaria") != null ? kiclient.getDetails().get("highRiskMalaria") : "-"));
        txt_HighRiskLabourSectionCesareaRecord.setText(humanize(kiclient.getDetails().get("HighRiskLabourSectionCesareaRecord") != null ? kiclient.getDetails().get("HighRiskLabourSectionCesareaRecord") : "-"));
        HighRiskPregnancyTooManyChildren.setText(humanize(kiclient.getDetails().get("HighRiskPregnancyTooManyChildren") != null ? kiclient.getDetails().get("HighRiskPregnancyTooManyChildren") : "-"));
        txt_highRiskHIVAIDS.setText(humanize(kiclient.getDetails().get("highRiskHIVAIDS") != null ? kiclient.getDetails().get("highRiskHIVAIDS") : "-"));


        txt_lbl_highRiskLabourFetusMalpresentation.setText(humanize(kiclient.getDetails().get("highRiskLabourFetusMalpresentation") != null ? kiclient.getDetails().get("highRiskLabourFetusMalpresentation") : "-"));
        txt_highRisklabourFetusNumber.setText(humanize(kiclient.getDetails().get("highRisklabourFetusNumber") != null ? kiclient.getDetails().get("highRisklabourFetusNumber") : "-"));
        txt_highRiskLabourFetusSize.setText(humanize(kiclient.getDetails().get("highRiskLabourFetusSize") != null ? kiclient.getDetails().get("highRiskLabourFetusSize") : "-"));
        txt_highRiskLabourTBRisk.setText(humanize(kiclient.getDetails().get("highRiskLabourTBRisk") != null ? kiclient.getDetails().get("highRiskLabourTBRisk") : "-"));
        highRiskPregnancyProteinEnergyMalnutrition.setText(humanize(kiclient.getDetails().get("highRiskPregnancyProteinEnergyMalnutrition") != null ? kiclient.getDetails().get("highRiskPregnancyProteinEnergyMalnutrition") : "-"));
        highRiskPregnancyPIH.setText(humanize(kiclient.getDetails().get("highRiskPregnancyPIH") != null ? kiclient.getDetails().get("highRiskPregnancyPIH") : "-"));
        txt_highRiskPregnancyDiabetes.setText(humanize(kiclient.getDetails().get("highRiskPregnancyDiabetes") != null ? kiclient.getDetails().get("highRiskPregnancyDiabetes") : "-"));
        txt_highRiskPregnancyAnemia.setText(humanize(kiclient.getDetails().get("highRiskPregnancyAnemia") != null ? kiclient.getDetails().get("highRiskPregnancyAnemia") : "-"));
        highRiskPostPartumSectioCaesaria.setText(humanize(kiclient.getDetails().get("highRiskPostPartumSectioCaesaria") != null ? kiclient.getDetails().get("highRiskPostPartumSectioCaesaria") : "-"));
        highRiskPostPartumForceps.setText(humanize(kiclient.getDetails().get("highRiskPostPartumForceps") != null ? kiclient.getDetails().get("highRiskPostPartumForceps") : "-"));
        highRiskPostPartumVacum.setText(humanize(kiclient.getDetails().get("highRiskPostPartumVacum") != null ? kiclient.getDetails().get("highRiskPostPartumVacum") : "-"));
        highRiskPostPartumPreEclampsiaEclampsia.setText(humanize(kiclient.getDetails().get("highRiskPostPartumPreEclampsiaEclampsia") != null ? kiclient.getDetails().get("highRiskPostPartumPreEclampsiaEclampsia") : "-"));
        highRiskPostPartumMaternalSepsis.setText(humanize(kiclient.getDetails().get("highRiskPostPartumMaternalSepsis") != null ? kiclient.getDetails().get("highRiskPostPartumMaternalSepsis") : "-"));
        highRiskPostPartumInfection.setText(humanize(kiclient.getDetails().get("highRiskPostPartumInfection") != null ? kiclient.getDetails().get("highRiskPostPartumInfection") : "-"));
        highRiskPostPartumHemorrhage.setText(humanize(kiclient.getDetails().get("highRiskPostPartumHemorrhage") != null ? kiclient.getDetails().get("highRiskPostPartumHemorrhage") : "-"));
        highRiskPostPartumPIH.setText(humanize(kiclient.getDetails().get("highRiskPostPartumPIH") != null ? kiclient.getDetails().get("highRiskPostPartumPIH") : "-"));
        highRiskPostPartumDistosia.setText(humanize(kiclient.getDetails().get("highRiskPostPartumDistosia") != null ? kiclient.getDetails().get("highRiskPostPartumDistosia") : "-"));

        show_risk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryFacade.logEvent("click_risk_detail");
                findViewById(R.id.id1).setVisibility(View.GONE);
                findViewById(R.id.id2).setVisibility(View.VISIBLE);
                findViewById(R.id.show_more_detail).setVisibility(View.VISIBLE);
                findViewById(R.id.show_more).setVisibility(View.GONE);
            }
        });

        show_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.id1).setVisibility(View.VISIBLE);
                findViewById(R.id.id2).setVisibility(View.GONE);
                findViewById(R.id.show_more).setVisibility(View.VISIBLE);
                findViewById(R.id.show_more_detail).setVisibility(View.GONE);
            }
        });

        kiview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryFacade.logEvent("taking_mother_pictures_on_kohort_ibu_detail_view");
                entityid = kiclient.entityId();

                if(hash.containsValue(entityid)){
                    updateMode = true;
                }
                // TODO create action : FR or Not

//                Intent takePictureIntent = new Intent(KIDetailActivity.this, SmartShutterActivity.class);
//                takePictureIntent.putExtra("org.sid.sidface.SmartShutterActivity.updated", updateMode);
//                takePictureIntent.putExtra("IdentifyPerson", false);
//                takePictureIntent.putExtra("org.sid.sidface.ImageConfirmation.id", entityid);
//                takePictureIntent.putExtra("org.sid.sidface.ImageConfirmation.origin", TAG); // send Class Name
//                startActivityForResult(takePictureIntent, 2);

            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, KMotherSmartRegisterActivity.class));
        overridePendingTransition(0, 0);
    }

    static String entityid;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        refresh
        Log.e(TAG, "onActivityResult: refresh" );
        finish();
        startActivity(getIntent());

    }
}