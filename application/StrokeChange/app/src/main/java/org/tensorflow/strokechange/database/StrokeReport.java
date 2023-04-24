package org.tensorflow.strokechange.database;

import java.sql.Time;
import java.sql.Timestamp;

public class StrokeReport {
    public String _ID;
    public String DateTime;
    public Double EyeSeverity;
    public Double MouthSeverity;

    public StrokeReport(String datetime, Double eye, Double mouth){
        this.DateTime = datetime;
        this.EyeSeverity = eye;
        this.MouthSeverity = mouth;
    }


}
