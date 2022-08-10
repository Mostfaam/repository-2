package com.xecan.chemobarcodescannerr;

/**
 * Created by femi on 3/12/18.
 */

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: byang
 * Date: May 24, 2007
 * Time: 2:27:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class Patient {
    private String name = null;
    private String attDoctorName = null;
    private String examroomName = null;
    private String RFID = null;
    private Date firstScanTime = null;
    private Date firstInTime = null;
    private Date lastRecordTime = null;
    private int waitTime = 0;
    private String appTime = "";
    private String appType = "";
    private String status = "3";
    private String color = null;
    private String arriveAt = null;
    private String ID = null;
    private String type = null;

    public Patient() {
        name = null;
        RFID = null;
        firstInTime = null;
        firstScanTime = null;
        lastRecordTime = null;
        waitTime = 0;
        examroomName = "";
        ID = "";
        status = "In";
        color = "";
        appTime = "";
        appType = "";
        type = "";
        attDoctorName = "";
    }

    // set appType
    public void setAppType(String appType) {
        this.appType = appType;
    }

    // get appType
    public String getAppType() {
        return this.appType;
    }

    // set patID1
    public void setType(String type) {
        this.type = type;
    }

    // get patID1
    public String getType() {
        return this.type;
    }

    // set patID1
    public void setID(String ID) {
        this.ID = ID;
    }

    // get patID1
    public String getID() {
        return this.ID;
    }

    // set color
    public void setColor(String color) {
        this.color = color;
    }

    // get color
    public String getColor() {
        return this.color;
    }

    // set patientName
    public void setName(String name) {
        this.name = name;
    }

    // get patientName
    public String getName() {
        return this.name;
    }

    // set status
    public void setStatus(String status) {
        this.status = status;
    }

    // get status
    public String getStatus() {
        return this.status;
    }

    // set exam room name
    public void setExamroomName(String examRoomName) {
        this.examroomName = examRoomName;
    }

    // get exam room name
    public String getExamroomName() {
        return this.examroomName;
    }

    // set patient wait time
    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    // get  patient wait time
    public int getWaitTime() {
        return this.waitTime;
    }

    // set patient wait time
    public void setAppTime(String appTime) {
        this.appTime = appTime;
    }

    // get  patient wait time
    public String getAppTime() {
        return this.appTime;
    }

    // set Patient RFID
    public void setRFID(String RFID) {
        this.RFID = RFID;
    }

    // get  patient RFID
    public String getRFID() {
        return this.RFID;
    }


    // set arriveAt
    public void setArriveAt(String arriveAt) {
        this.arriveAt = arriveAt;
    }

    // get  arriveAt
    public String getArriveAt() {
        return this.arriveAt;
    }

    // set patient Date
    public void setFirstInTime(Date firstInTime) {
        this.firstInTime = firstInTime;
    }

    // get patient date
    public Date getFirstInTime() {
        return this.firstInTime;
    }

    // set patient scan Date
    public void setFirstScanTime(Date firstScanTime) {
        this.firstScanTime = firstScanTime;
    }

    // get patient scan date
    public Date getFirstScanTime() {
        return this.firstScanTime;
    }


    // set patient record Date
    public void setLastRecordTime(Date lastRecordTime) {
        this.lastRecordTime = lastRecordTime;
    }

    // get patient date
    public Date getLastRecordTime() {
        return this.lastRecordTime;
    }

    // set att doc name
    public void setAttDoctorName(String attDoctorName) {
        this.attDoctorName = attDoctorName;
    }

    // get att doc name
    public String getAttDoctorName() {
        return this.attDoctorName;
    }
}
