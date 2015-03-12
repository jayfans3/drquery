package com.asiainfo.billing.drquery.process.core.request;

/**
 *
 * @author zhouquan3
 * Jun 21 2012
 */
public class DRCheckQueryProcessRequest {
    
    private String billId;//计费号码
    private String[] destId;//对端号码数组

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public void setDestId(String[] destId) {
        this.destId = destId;
    }

    public String getBillId() {
        return billId;
    }

    public String[] getDestId() {
        return destId;
    }
    
    
}
