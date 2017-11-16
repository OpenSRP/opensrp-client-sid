package org.smartregister.bidan_cloudant.option;

import org.smartregister.Context;
import org.smartregister.bidan_cloudant.R;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.view.dialog.ServiceModeOption;

import static org.smartregister.view.activity.SecuredNativeSmartRegisterActivity.ClientsHeaderProvider;

public class ChildServiceModeOption extends ServiceModeOption {

    public ChildServiceModeOption(SmartRegisterClientsProvider provider) {
        super(provider);
    }

    @Override
    public String name() {
        return Context.getInstance().getStringResource(R.string.child_title);
    }

    @Override
    public ClientsHeaderProvider getHeaderProvider() {
        return new ClientsHeaderProvider() {
            @Override
            public int count() { return 6; }

            @Override
            public int weightSum() {
                return 100;
            }

            @Override
            public int[] weights() {
                return new int[]{26, 16, 15, 15, 23, 8};
            }

            @Override
            public int[] headerTextResourceIds() {
                return new int[]{
                        R.string.header_name, R.string.header_dok_persalinan, R.string.str_header_neonatal,
                        R.string.str_child_immunizations, R.string.header_birth_status, R.string.header_edit
                };
            }
        };
    }


}
