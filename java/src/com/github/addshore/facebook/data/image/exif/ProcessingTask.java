package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProcessingTask extends Task {

    private TextArea textArea;
    private File dir;
    private File exiftoolFile;
    private String stateMessage;
    private Boolean debugOutput;
    private Boolean dryRun;

    ProcessingTask(TextArea textArea, File dir, File exiftoolFile, String initialStateMessage, Boolean debugOutput, Boolean dryRun){
        this.textArea = textArea;
        this.dir = dir;
        this.exiftoolFile = exiftoolFile;
        this.stateMessage = initialStateMessage;
        this.debugOutput = debugOutput;
        this.dryRun = dryRun;
    }

    private void appendMessage( String string ) {
        System.out.println("ProcessingTask: " + string);
        stateMessage = stateMessage + "\n" + string;
        updateUI();
    }

    private void appendDebugMessage( String string ) {
        string = "debug: " + string;
        if ( this.debugOutput ) {
            this.appendMessage(  string );
        } else {
            System.out.println("ProcessingTask: " + string);
        }
    }

    private void updateUI() {
        Runnable updater = new Runnable() {
            @Override
            public void run() {
                updateMessage( stateMessage );
                textArea.setText(stateMessage);
                // Trigger the listener that makes the field scrollable?
                // https://stackoverflow.com/questions/17799160/javafx-textarea-and-autoscroll
                textArea.appendText("");
            }
        };

        // UI update is run on the Application thread
        Platform.runLater(updater);
    }

    @Override
    protected Object call() throws Exception {
        // Find all album json files
        appendMessage( "Looking for albums..." );
        File albumDir = new File( dir.toPath().toString() + File.separator + "album" );
        appendDebugMessage("In album dir: " + albumDir.getPath());

        File[] albumJsonFiles = albumDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".json"); }
        } );
        File[] albumHtmlFiles = albumDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".html"); }
        } );

        appendMessage(albumJsonFiles.length + " JSON album files found");
        appendMessage(albumHtmlFiles.length + " HTML album files found");

        // Stop if we detected no JSON but did find HTML
        if( albumJsonFiles.length == 0 && albumHtmlFiles.length != 0 ) {
            appendMessage("This program currently only works with the JSON facebook downloads");
            return null;
        }

        ExifToolBuilder builder = new ExifToolBuilder();
        builder.withPath( exiftoolFile );

        ExifTool exifTool = builder.build();

        // Process the album
        for (File albumJsonFile : albumJsonFiles) {
            InputStream inputStream = new FileInputStream(albumJsonFile);

            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String jsonTxt = writer.toString();
            JSONObject albumJson = new JSONObject(jsonTxt);
            if (!albumJson.has("photos")) {
                appendDebugMessage("Album has no photos");
                continue;
            }

            JSONArray albumPhotos = albumJson.getJSONArray("photos");

            appendMessage("Album: " + albumJson.getString("name") + ", " + albumPhotos.length() + " photos");

            // Process the photos in the album
            for (int i = 0; i < albumPhotos.length(); i++) {
                JSONObject photoData = albumPhotos.getJSONObject(i);
                appendMessage(" - Processing " + photoData.getString("uri"));
                JSONObject photoMetaData = photoData.getJSONObject("media_metadata").getJSONObject("photo_metadata");

                // Figure out the time the picture was taken
                String takenTimestamp;
                if (photoMetaData.has("taken_timestamp")) {
                    // Keep timestamp as is
                    takenTimestamp = photoMetaData.getString("taken_timestamp");
                } else if (photoMetaData.has("modified_timestamp")) {
                    // It's missing, replace with modified
                    takenTimestamp = photoMetaData.getString("modified_timestamp");
                } else {
                    // Fallback to the creation timestamp
                    takenTimestamp = photoMetaData.getString("creation_timestamp");
                }
                takenTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date(Long.parseLong(takenTimestamp) * 1000));

                // And set a modified timestamp
                String modifiedTimestamp;
                if (photoMetaData.has("modified_timestamp")) {
                    modifiedTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date(Long.parseLong(photoMetaData.getString("modified_timestamp")) * 1000));
                } else {
                    modifiedTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
                }

                String fStop = null;
                if (photoMetaData.has("f_stop")) {
                    String[] parts = photoMetaData.getString("f_stop").split("/");
                    if (parts.length > 1) {
                        fStop = Double.toString(Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]));
                    } else {
                        fStop = photoMetaData.getString("f_stop");
                    }
                }

                File imageFile = new File(dir.getParentFile().toPath().toString() + File.separator + photoData.getString("uri"));

                Map<Tag, String> exifData = new HashMap<Tag, String>();

                exifData.put( CustomTag.MODIFYDATE, modifiedTimestamp );
                exifData.put( StandardTag.DATE_TIME_ORIGINAL, takenTimestamp );

                if( photoMetaData.has("camera_make") ) {
                    exifData.put( StandardTag.MAKE, photoMetaData.getString("camera_make") );
                }
                if( photoMetaData.has("camera_model") ) {
                    exifData.put( StandardTag.MODEL, photoMetaData.getString("camera_model") );
                }

                if( photoMetaData.has("latitude") ) {
                    exifData.put( StandardTag.GPS_LATITUDE, photoMetaData.getString("latitude") );
                    exifData.put( StandardTag.GPS_LATITUDE_REF, photoMetaData.getString("latitude") );
                    exifData.put( StandardTag.GPS_LONGITUDE, photoMetaData.getString("longitude") );
                    exifData.put( StandardTag.GPS_LONGITUDE_REF, photoMetaData.getString("longitude") );
                    exifData.put( StandardTag.GPS_ALTITUDE, "0" );
                    exifData.put( StandardTag.GPS_ALTITUDE_REF, "0" );
                }

                if( photoMetaData.has("exposure") ) {
                    exifData.put( CustomTag.EXPOSURE, photoMetaData.getString("exposure") );
                }
                if( photoMetaData.has("iso_speed") ) {
                    exifData.put( StandardTag.ISO, photoMetaData.getString("iso_speed") );
                }
                if( photoMetaData.has("focal_length") ) {
                    exifData.put( StandardTag.FOCAL_LENGTH, photoMetaData.getString("focal_length") );
                }
                if(fStop != null) {
                    exifData.put( CustomTag.FNUMBER, fStop );
                }

                if(!this.dryRun){
                    appendDebugMessage("calling setImageMeta for " + photoData.getString("uri"));
                    exifTool.setImageMeta( imageFile, exifData );
                }

            }
        }

        appendMessage("Done!!");

        exifTool.close();

        return null;
    }

}
