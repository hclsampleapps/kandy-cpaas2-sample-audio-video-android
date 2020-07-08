package com.hcl.kandy.cpass.call;

import com.rbbn.cpaas.mobile.call.api.CallState;

interface CPaaSCallFragment {
    void callStateChanged(CallState.Type state);

    void callOperationPerformed(String callOperation);
}
