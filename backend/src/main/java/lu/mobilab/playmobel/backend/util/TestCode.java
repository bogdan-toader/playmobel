package lu.mobilab.playmobel.backend.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Created by bogdan.toader on 13/12/16.
 */
public class TestCode {
    public static void main(String[] arg){

        String dateStr="2008-10-23 02:53:04";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
        ZoneOffset zoneOffset = ZoneId.of("GMT").getRules().getOffset(dateTime);
        long timestamp = dateTime.toEpochSecond(zoneOffset)*1000; //to get in ms

        System.out.println(timestamp);




    }
}
