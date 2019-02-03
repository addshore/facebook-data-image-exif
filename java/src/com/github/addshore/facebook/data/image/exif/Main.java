package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        // If Debug?
        //System.setProperty("exiftool.debug","True");

        File exifTool = null;
        try{
            exifTool = this.getExifToolFromPath();
        } catch ( FileNotFoundException ignored) {
            if( this.isWindows() ) {
                // Get exiftool from the JAR if we are on windows
                exifTool = JarredFile.getFileFromJar( "exiftool.exe" );
            }
        }

        stage.setTitle("Facebook Data Image Exif");
        Scene dataEntryScene = this.getDataEntryScene( stage, exifTool );

        stage.setScene( dataEntryScene );
        stage.show();
    }

    private Boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private File getExifToolFromPath() throws FileNotFoundException {
        for (String dirString: System.getenv("PATH").split(System.getProperty("path.separator"))) {
            File dir = new File(dirString);
            if ( dir.isDirectory() ) {
                for ( File file: dir.listFiles() ) {
                    String fileWithoutExt = FilenameUtils.removeExtension(file.getName());
                    if (fileWithoutExt.equals("exiftool")) {
                        return file;
                    }
                }
            }
        }
        throw new FileNotFoundException();
    }

    private Scene getDataEntryScene(final Stage stage, File exifTool) throws Exception {
        GridPane dataEntryView = FXMLLoader.load(getClass().getResource("dataEntry.fxml"));

        final TextField dirInput = (TextField) dataEntryView.getChildren().get(1);
        final TextField toolInput = (TextField) dataEntryView.getChildren().get(3);
        Button button = (Button) dataEntryView.getChildren().get(2);

        // If we found the exiftool in PATH then preset it and lock the box
        if ( exifTool != null ) {
            toolInput.setText(exifTool.getAbsolutePath());
            toolInput.setEditable(false);
        }

        if( System.getProperty("os.name").toLowerCase().contains("windows") ){
            dirInput.setPromptText( "C:\\Users\\addshore\\downloads\\facebook-export\\photos" );
        } else {
            dirInput.setPromptText("/path/to/facebook/export/photos/directory");
        }

        button.setOnAction(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent t){

                String exifToolString = toolInput.getText();
                File exiftoolFile = new File(exifToolString);

                if(!exiftoolFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Can't find exiftool file specified", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String dirPathString = dirInput.getText();

                if( dirPathString.length() < "photos_and_videos".length() || !dirPathString.substring(dirPathString.length() - "photos_and_videos".length()).equals("photos_and_videos")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Directory must be your photos_and_videos directory", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                File dirFile = new File(dirPathString);
                if(!dirFile.exists() || !dirFile.isDirectory()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Directory does not exist. ", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                try {
                    showProcessingScreen(stage, dirFile, exiftoolFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        });

        return new Scene(dataEntryView, 400, 200);
    }

    private void showProcessingScreen( Stage stage, File dir, File exiftoolFile ) throws Exception {
        TextFlow textArea = new TextFlow();
        textArea.getChildren().add( new Text( "Processing... " ) );

        stage.setScene(new Scene(textArea, 800, 800));
        stage.show();

        doProcessing( textArea, dir, exiftoolFile );
    }

    /**
     * @param textArea for output
     */
    private void doProcessing( TextFlow textArea, File dir, File exiftoolFile ) throws Exception {

        // Find all album json files
        textArea.getChildren().add( new Text( "Looking for albums... " ) );
        File albumDir = new File( dir.toPath().toString() + "\\album" );


        File[] albumJsonFiles = albumDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return filename.endsWith(".json"); }
        } );

        textArea.getChildren().add( new Text( albumJsonFiles.length + " albums found!\n" ) );

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

            textArea.getChildren().add(new Text("=== Album " + albumJson.getString("name") + " ===\n"));

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

                textArea.getChildren().add(new Text(" * Processing: " + photoData.getString("uri") + "\n"));

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

        textArea.getChildren().add(new Text(" Done!!\n"));
        exifTool.close();

    }

}
