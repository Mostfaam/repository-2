package com.xecan.chemobarcodescannerr;

public class Chair {
    private String id;
    private String ChairName;
    private String Type;
    private String ChairStatus;
    private String AcctNum;
    private String scannerID;
    private String postUrl;
    private String simDrugsJSon;
    private String verifiedDrugsJSon;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChairName() {
        return ChairName;
    }

    public void setChairName(String chairName) {
        ChairName = chairName;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getChairStatus() {
        return ChairStatus;
    }

    public void setChairStatus(String chairStatus) {
        ChairStatus = chairStatus;
    }

    public String getAcctNum() {
        return AcctNum;
    }

    public void setAcctNum(String acctNum) {
        AcctNum = acctNum;
    }

    public String getScannerID() {
        return scannerID;
    }

    public void setScannerID(String scannerID) {
        this.scannerID = scannerID;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getSimDrugsJSon() {
        return simDrugsJSon;
    }

    public void setSimDrugsJSon(String simDrugsJSon) {
        this.simDrugsJSon = simDrugsJSon;
    }

    public String getVerifiedDrugsJSon() {
        return verifiedDrugsJSon;
    }

    public void setVerifiedDrugsJSon(String verifiedDrugsJSon) {
        this.verifiedDrugsJSon = verifiedDrugsJSon;
    }

    @Override
    public String toString() {
        return "Chair{" +
                "id='" + id + '\'' +
                ", ChairName='" + ChairName + '\'' +
                ", Type='" + Type + '\'' +
                ", ChairStatus='" + ChairStatus + '\'' +
                ", AcctNum='" + AcctNum + '\'' +
                ", scannerID='" + scannerID + '\'' +
                ", postUrl='" + postUrl + '\'' +
                ", simDrugsJSon='" + simDrugsJSon + '\'' +
                ", verifiedDrugsJSon='" + verifiedDrugsJSon + '\'' +
                '}';
    }
}

