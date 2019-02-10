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

    ProcessingTask(TextArea textArea, File dir, File exiftoolFile){
        this.textArea = textArea;
        this.dir = dir;
        this.exiftoolFile = exiftoolFile;
        this.stateMessage = "Task started!";
    }

    private void appendMessage( String string ) {
        System.out.println("ProcessingTask: " + string);
        stateMessage = stateMessage + "\n" + string;
        updateUI();
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
        appendMessage( "Looking for albums... " );
        File albumDir = new File( dir.toPath().toString() + "\\album" );

        File[] albumJsonFiles = albumDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".json"); }
        } );

        appendMessage(albumJsonFiles.length + " albums found!");

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
                continue;
            }

            appendMessage("=== Album " + albumJson.getString("name") + " ===");

            // Process the photos in the album
            JSONArray albumPhotos = albumJson.getJSONArray("photos");
            for (int i = 0; i < albumPhotos.length(); i++) {
                JSONObject photoData = albumPhotos.getJSONObject(i);
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

                File imageFile = new File(dir.toPath().toString() + "/../" + photoData.getString("uri"));

                appendMessage(" * Processing: " + photoData.getString("uri"));

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

                exifData.put( StandardTag.COMMENT, "EXIF data converted from facebook dump using https://github.com/addshore/facebook-data-image-exif" );

                exifTool.setImageMeta( imageFile, exifData );

            }
        }

        appendMessage(" Done!!");

        exifTool.close();

        return null;
    }

}
