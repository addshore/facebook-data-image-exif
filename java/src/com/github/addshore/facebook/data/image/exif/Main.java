package com.github.addshore.facebook.data.image.exif;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;


public class Main extends Application {

    private String version;
    private File existingExifTool;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        // If Debug?
        //System.setProperty("exiftool.debug","True");

        this.setMainVersionFromPom();
        this.setExistingExifToolFile();

        stage.setTitle("Facebook Data Image Exif Tool");
        Scene dataEntryScene = this.getDataEntryScene( stage );

        stage.setScene( dataEntryScene );
        stage.show();
    }

    private void setMainVersionFromPom() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader("pom.xml"));
        this.version = model.getVersion();
    }

    private void setExistingExifToolFile() {
        // Try to return an exiftool from the path
        try{
            this.existingExifTool = this.getExifToolFromPath();
        } catch ( FileNotFoundException ignored) {}

        // Get exiftool from the JAR if we are on windows and it is packaged
        if( this.isWindows() ) {
            try {
                this.existingExifTool = JarredFile.getFileFromJar( "exiftool.exe" );
            } catch (URISyntaxException | IOException e) {
                showErrorThenClose("Packaged exiftool.exe issue :\n\n" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private void showErrorThenClose( String message ) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
        Platform.exit();
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

    private Scene getDataEntryScene(final Stage stage) throws Exception {
        GridPane dataEntryView = FXMLLoader.load(getClass().getResource("dataEntry.fxml"));

        final TextField dirInput = (TextField) dataEntryView.getChildren().get(1);
        final TextField toolInput = (TextField) dataEntryView.getChildren().get(3);
        final Label versionLabel = (Label) dataEntryView.getChildren().get(5);
        final Hyperlink hyperLinkAddshore = (Hyperlink) dataEntryView.getChildren().get(6);
        final Hyperlink hyperLinkExif = (Hyperlink) dataEntryView.getChildren().get(7);
        final CheckBox debugCheckbox = (CheckBox) dataEntryView.getChildren().get(8);
        Button button = (Button) dataEntryView.getChildren().get(2);

        versionLabel.setText("Version: " + this.version);

        hyperLinkAddshore.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                getHostServices().showDocument("https://addshore.com/redirects/exiftool/writtenbylink");
            }
        });
        hyperLinkExif.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                getHostServices().showDocument("https://sno.phy.queensu.ca/~phil/exiftool/");
            }
        });

        // If we found the exiftool in PATH then preset it and lock the box
        if ( this.existingExifTool != null ) {
            toolInput.setText(this.existingExifTool.getAbsolutePath());
            toolInput.setEditable(false);
        }

        if( System.getProperty("os.name").toLowerCase().contains("windows") ){
            dirInput.setPromptText( "C:\\Users\\addshore\\downloads\\facebook-export\\photos_and_videos" );
        } else {
            dirInput.setPromptText("/path/to/facebook/export/photos_and_videos/directory");
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
                    final TextArea textArea = new TextArea();

                    textArea.textProperty().addListener(new ChangeListener<Object>() {
                        @Override
                        public void changed(ObservableValue<?> observable, Object oldValue,
                                            Object newValue) {
                            textArea.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
                            //use Double.MIN_VALUE to scroll to the top
                        }
                    });

                    stage.setScene(new Scene(textArea, 800, 500));
                    stage.show();

                    String initialStateMessage = "Version: " + version + "\n" +
                            "OS: " + System.getProperty("os.name") + "\n" +
                            "-------------------------------------------------";

                    ProcessingTask task = new ProcessingTask( textArea, dirFile, exiftoolFile, initialStateMessage, debugCheckbox.isSelected() );
                    Thread th = new Thread(task);
                    th.setDaemon(false);
                    System.out.println("Main: pre thread start");
                    th.start();
                    System.out.println("Main: post thread start");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return new Scene(dataEntryView, 400, 300);
    }

}
