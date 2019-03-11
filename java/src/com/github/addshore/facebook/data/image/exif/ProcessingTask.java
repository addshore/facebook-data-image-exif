package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProcessingTask extends Task {

    private TextArea textArea;
    private File dir;
    private ExifTool exifTool;
    private String stateMessage;
    private Boolean debugOutput;
    private Boolean dryRun;

    ProcessingTask(TextArea textArea, File dir, ExifTool exifTool, String initialStateMessage, Boolean debugOutput, Boolean dryRun){
        this.textArea = textArea;
        this.dir = dir;
        this.exifTool = exifTool;
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
        try{
            processTask();
        } catch( JSONException | IOException exception ) {
            appendMessage("Something went wrong while running the task.");
            appendMessage("ERROR: " + exception.getMessage() );
            appendMessage("Task may not have completely finished.");
        }

        // We used to close exif tool here, but instead just leave it running until the user closes the app.
        // I should probably just figure out how to make sure that all tasks in the pool have finished before closing...

        return null;
    }

    private void processTask() throws IOException, JSONException {
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
            return;
        }

        int statProcessedImages = 0;
        int statFailedImages = 0;

        // Process the album
        for (File albumJsonFile : albumJsonFiles) {
            appendDebugMessage("Loading album file " + albumJsonFile.getPath());
            InputStream inputStream = new FileInputStream(albumJsonFile);

            appendDebugMessage("Writing to internal string");
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String jsonTxt = writer.toString();
            appendDebugMessage("Loading album object");
            JSONObject albumJson = new JSONObject(jsonTxt);
            if (!albumJson.has("photos")) {
                appendDebugMessage("Album has no photos");
                continue;
            }

            appendDebugMessage("Getting album photos");
            JSONArray albumPhotos = albumJson.getJSONArray("photos");

            appendMessage("Album: " + albumJson.getString("name") + ", " + albumPhotos.length() + " photos");

            // Process the photos in the album
            for (int i = 0; i < albumPhotos.length(); i++) {
                appendDebugMessage("Getting photo data: " + i);
                JSONObject photoData = albumPhotos.getJSONObject(i);

                appendMessage(" - Processing " + photoData.getString("uri"));
                try{
                    if( processFile( photoData ) ) {
                        statProcessedImages++;
                    } else {
                        statFailedImages++;
                    }
                } catch( JSONException jsonException ) {
                    statFailedImages++;
                    appendMessage("Something went wrong while getting data for the image.");
                    appendMessage("ERROR: " + jsonException.getMessage() );
                    appendMessage("Image has not been processed entirely");
                } catch( IOException ioException ) {
                    statFailedImages++;
                    appendMessage("Something went wrong while writing data to the image.");
                    appendMessage("ERROR: " + ioException.getMessage() );
                    appendMessage("Image has not been processed entirely");
                }
            }
        }

        appendMessage("-------------------------------------------------");
        appendMessage("Task complete");
        appendMessage("Images processed: " + statProcessedImages);
        appendMessage("Images failed: " + statFailedImages);
        if(statFailedImages != 0) {
            appendMessage("See the full output for detailed failure reasons...");
        }
    }

    private Boolean processFile( JSONObject photoData ) throws JSONException, IOException {
        File imageFile = new File(dir.getParentFile().toPath().toString() + File.separator + photoData.getString("uri"));
        appendDebugMessage("Image file path: " + imageFile.getPath());

        if( !imageFile.exists() ) {
            appendMessage( "ERROR: the file does not exist in the expected location. Is your download complete?" );
            return false;
        }
        if( !imageFile.canWrite() ) {
            appendMessage( "ERROR: the file is not writable." );
            return false;
        }

        JSONObject photoMetaData = null;
        if(photoData.has("media_metadata")) {
            JSONObject mediaMetaData = photoData.getJSONObject("media_metadata");
            if( mediaMetaData.has("photo_metadata") ) {
                photoMetaData = mediaMetaData.getJSONObject("photo_metadata");
            } else {
                appendDebugMessage("WARNING: Got media_metadata but no photo_metadata, FAILING for image...");
            }
        } else {
            // XXX: should we actually assume this? It's a shame the dump format isn't well documented...
            if(photoData.has("creation_timestamp")) {
                // If this high level element has the creation_timestamp then assume it as the photo meta data?
                photoMetaData = photoData;
                appendDebugMessage("Falling back to root meta data for image");
            } else {
                appendDebugMessage("WARNING: No media_metadata found, and no fallback used, FAILING for image...");
            }
        }

        if(photoMetaData == null) {
            appendMessage("Skipping image (due to no meta data found)");
            return false;
        }

        // Figure out the time the picture was taken
        String takenTimestamp = null;
        if (photoMetaData.has("taken_timestamp")) {
            // Keep timestamp as is
            takenTimestamp = photoMetaData.getString("taken_timestamp");
            appendDebugMessage(StandardTag.DATE_TIME_ORIGINAL + " got from taken_timestamp:" + takenTimestamp);
        } else if (photoMetaData.has("modified_timestamp")) {
            // It's missing, replace with modified
            takenTimestamp = photoMetaData.getString("modified_timestamp");
            appendDebugMessage(StandardTag.DATE_TIME_ORIGINAL + " got from modified_timestamp:" + takenTimestamp);
        } else if(photoMetaData.has("creation_timestamp")) {
            // Fallback to the creation timestamp
            takenTimestamp = photoMetaData.getString("creation_timestamp");
            appendDebugMessage(StandardTag.DATE_TIME_ORIGINAL + " got from creation_timestamp:" + takenTimestamp);
        } else {
            appendDebugMessage(StandardTag.DATE_TIME_ORIGINAL + " could not find a source");
        }
        if(takenTimestamp != null) {
            takenTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date(Long.parseLong(takenTimestamp) * 1000));
        }

        // And set a modified timestamp
        String modifiedTimestamp;
        if (photoMetaData.has("modified_timestamp")) {
            modifiedTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date(Long.parseLong(photoMetaData.getString("modified_timestamp")) * 1000));
            appendDebugMessage(CustomTag.MODIFYDATE + " got from modified_timestamp:" + photoMetaData.getString("modified_timestamp"));
        } else {
            modifiedTimestamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
            appendDebugMessage(CustomTag.MODIFYDATE + " could not find a source, using today");
        }

        // fstop
        String fStop = null;
        if (photoMetaData.has("f_stop")) {
            String[] parts = photoMetaData.getString("f_stop").split("/");
            if (parts.length > 1) {
                fStop = Double.toString(Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]));
            } else {
                fStop = photoMetaData.getString("f_stop");
            }
            appendDebugMessage(CustomTag.FNUMBER + " got data " + fStop);
        } else {
            appendDebugMessage(CustomTag.FNUMBER + " could not find data");
        }

        appendDebugMessage("Constructing exif data object");
        Map<Tag, String> exifData = new HashMap<Tag, String>();

        exifData.put( CustomTag.MODIFYDATE, modifiedTimestamp );

        if( takenTimestamp != null ) {
            exifData.put( StandardTag.DATE_TIME_ORIGINAL, takenTimestamp );
        }

        if( photoMetaData.has("camera_make") ) {
            exifData.put( StandardTag.MAKE, photoMetaData.getString("camera_make") );
            appendDebugMessage(StandardTag.MAKE + " got data " + photoMetaData.getString("camera_make"));
        } else {
            appendDebugMessage(StandardTag.MAKE + " could not find data");
        }
        if( photoMetaData.has("camera_model") ) {
            exifData.put( StandardTag.MODEL, photoMetaData.getString("camera_model") );
            appendDebugMessage(StandardTag.MODEL + " got data " + photoMetaData.getString("camera_model"));
        } else {
            appendDebugMessage(StandardTag.MODEL + " could not find data");
        }

        if( photoMetaData.has("latitude") && photoMetaData.has("longitude") ) {
            exifData.put( StandardTag.GPS_LATITUDE, photoMetaData.getString("latitude") );
            exifData.put( StandardTag.GPS_LATITUDE_REF, photoMetaData.getString("latitude") );
            exifData.put( StandardTag.GPS_LONGITUDE, photoMetaData.getString("longitude") );
            exifData.put( StandardTag.GPS_LONGITUDE_REF, photoMetaData.getString("longitude") );
            exifData.put( StandardTag.GPS_ALTITUDE, "0" );
            exifData.put( StandardTag.GPS_ALTITUDE_REF, "0" );
            appendDebugMessage(StandardTag.GPS_LATITUDE + " got data " + photoMetaData.getString("latitude"));
            appendDebugMessage(StandardTag.GPS_LONGITUDE + " got data " + photoMetaData.getString("longitude"));
        } else {
            appendDebugMessage("COORDINATES could not find data");
        }

        if( photoMetaData.has("exposure") ) {
            exifData.put( CustomTag.EXPOSURE, photoMetaData.getString("exposure") );
            appendDebugMessage(CustomTag.EXPOSURE + " got data " + photoMetaData.getString("exposure"));
        } else {
            appendDebugMessage(CustomTag.EXPOSURE + " could not find data");
        }
        if( photoMetaData.has("iso_speed") ) {
            exifData.put( StandardTag.ISO, photoMetaData.getString("iso_speed") );
            appendDebugMessage(StandardTag.ISO + " got data " + photoMetaData.getString("iso_speed"));
        } else {
            appendDebugMessage(StandardTag.ISO + " could not find data");
        }
        if( photoMetaData.has("focal_length") ) {
            exifData.put( StandardTag.FOCAL_LENGTH, photoMetaData.getString("focal_length") );
            appendDebugMessage(StandardTag.FOCAL_LENGTH + " got data " + photoMetaData.getString("focal_length"));
        } else {
            appendDebugMessage(StandardTag.FOCAL_LENGTH + " could not find data");
        }
        if(fStop != null) {
            exifData.put( CustomTag.FNUMBER, fStop );
        }

        if(!this.dryRun){
            appendDebugMessage("calling setImageMeta for " + photoData.getString("uri"));
            exifTool.setImageMeta( imageFile, exifData );
        } else {
            appendDebugMessage("skipping setImageMeta for " + photoData.getString("uri") + " (dryrun)");
        }

        return true;
    }

}
