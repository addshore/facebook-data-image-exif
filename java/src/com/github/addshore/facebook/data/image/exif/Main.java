package com.github.addshore.facebook.data.image.exif;

import javafx.application.Application;
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
import java.util.Objects;


public class Main extends Application {

    private String version;
    private TextField toolInput;
    private TextField dirInput;
    private CheckBox debugCheckbox;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        // If Debug?
        //System.setProperty("exiftool.debug","True");

        this.setMainVersionFromPom();
        this.stage = stage;

        stage.setTitle("Facebook Data Image Exif Tool");
        Scene dataEntryScene = this.getDataEntryScene( stage );

        stage.setScene( dataEntryScene );
        stage.show();
    }

    private File getPomFile() {
        File file =  new File("pom.xml");
        if( file.exists() ) {
            return file;
        }

        try {
            return JarredFile.getFileFromJar("pom.xml");
        } catch ( URISyntaxException | IOException e ) {
            e.printStackTrace();
        }

        return null;
    }

    private void setMainVersionFromPom() throws IOException, XmlPullParserException {
        File pom = getPomFile();
        if(pom != null) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pom));
            this.version = model.getVersion();
        } else {
            this.version = "unknown";
        }
    }

    private File getExifToolFromPath() throws FileNotFoundException {
        for (String dirString: System.getenv("PATH").split(System.getProperty("path.separator"))) {
            File dir = new File(dirString);
            if ( dir.isDirectory() ) {
                for ( File file: Objects.requireNonNull(dir.listFiles())) {
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

        // Get element objects from the UI
        dirInput = (TextField) dataEntryView.getChildren().get(1);
        final Label toolLabel = (Label) dataEntryView.getChildren().get(2);
        toolInput = (TextField) dataEntryView.getChildren().get(3);

        // Details grid pain
        final GridPane linksGrid = (GridPane) dataEntryView.getChildren().get(4);
        final Label versionLabel = (Label) linksGrid.getChildren().get(0);
        final Hyperlink hyperLinkAddshore = (Hyperlink) linksGrid.getChildren().get(1);
        final Hyperlink hyperLinkExif = (Hyperlink) linksGrid.getChildren().get(2);

        // Submission grid
        final GridPane submitGrid = (GridPane) dataEntryView.getChildren().get(6);
        Button runButton = (Button) submitGrid.getChildren().get(0);
        Button dryRunButton = (Button) submitGrid.getChildren().get(1);
        debugCheckbox = (CheckBox) submitGrid.getChildren().get(2);

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
                getHostServices().showDocument("https://addshore.com/redirects/exiftool/exiftoollink");
            }
        });

        if( System.getProperty("os.name").toLowerCase().contains("windows") ){
            dirInput.setPromptText( "C:\\Users\\addshore\\downloads\\facebook-export\\photos_and_videos" );
        } else {
            dirInput.setPromptText("/path/to/facebook/export/photos_and_videos/directory");
        }

        if( System.getProperty("os.name").toLowerCase().contains("windows") ){
            toolInput.setPromptText( "C:\\Users\\addshore\\downloads\\exiftool.exe" );
        } else {
            toolInput.setPromptText("/usr/bin/exiftool");
        }

        try {
            final File exifToolFromPath = getExifToolFromPath();
            toolInput.setText(exifToolFromPath.getAbsolutePath());
            toolLabel.setText(toolLabel.getText() + " (found in your PATH)");
        } catch( FileNotFoundException ignored ){
            toolLabel.setText(toolLabel.getText() + " (downloadable below)");
        }

        runButton.setOnAction(this.getButtonClickEventHandler(false));
        dryRunButton.setOnAction(this.getButtonClickEventHandler(true));

        return new Scene(dataEntryView, 400, 300);
    }

    private EventHandler<ActionEvent> getButtonClickEventHandler( Boolean dryRun ) {
        return new EventHandler<ActionEvent>(){

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
                            "Debug: " + debugCheckbox.isSelected() + "\n" +
                            "Dry run: " + dryRun + "\n" +
                            "-------------------------------------------------";

                    ProcessingTask task = new ProcessingTask(
                            textArea,
                            dirFile,
                            exiftoolFile,
                            initialStateMessage,
                            debugCheckbox.isSelected(),
                            dryRun
                    );
                    Thread th = new Thread(task);
                    th.setDaemon(false);
                    System.out.println("Main: pre thread start");
                    th.start();
                    System.out.println("Main: post thread start");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

}
