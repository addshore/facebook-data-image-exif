package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;


public class Main extends Application {

    private String version = "0.8";
    private TextField toolInput;
    private TextField dirInput;
    private CheckBox debugCheckbox;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception{
        this.stage = stage;

        stage.setTitle("Facebook Data Image Exif Tool");
        Scene dataEntryScene = this.getDataEntryScene( stage );

        //System.setProperty("exiftool.debug","True");

        stage.setScene( dataEntryScene );
        stage.show();
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

    private Boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
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

        if( isWindows() ){
            dirInput.setPromptText( "C:\\Users\\example\\downloads\\extracted-facebook-export" );
            toolInput.setPromptText( "C:\\Users\\example\\downloads\\exiftool.exe" );

        } else {
            dirInput.setPromptText("/path/to/extracted-facebook-export");
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

            private File getPhotosDirFromInput( String input ) {
                File inputFile = new File( dirInput.getText() );

                if( inputFile.getPath().endsWith("photos_and_videos") ) {
                    return inputFile;
                }

                return new File( inputFile.getPath() + File.separator + "photos_and_videos" );
            }

            @Override
            public void handle(ActionEvent t){
                if( toolInput.getText().isEmpty() || dirInput.getText().isEmpty() ) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Both fields must be filled", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                File exiftoolFile = new File(toolInput.getText());
                if(!exiftoolFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Can't find exiftool file specified", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                if(isWindows() && !exiftoolFile.getPath().endsWith("exiftool.exe")) {
                    // If on windows and we have been given the dir instead of exe file, add the exe to the path
                    exiftoolFile = new File(exiftoolFile.getPath() + File.separator + "exiftool.exe");
                }
                if(isWindows() && !exiftoolFile.getPath().endsWith("exiftool.exe")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please rename your exiftool exe to exiftool.exe", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                File dirFile = getPhotosDirFromInput( dirInput.getText() );
                if(!dirFile.exists() || !dirFile.isDirectory()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Directory does not exist: " + dirFile.getPath(), ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                try {
                    final TextArea textArea = new TextArea();
                    textArea.setText("Task is starting...");

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

                    ExifToolBuilder builder = new ExifToolBuilder();
                    builder.withPath( exiftoolFile );
                    ExifTool exifTool = builder.build();

                    String initialStateMessage = "Version: " + version + "\n" +
                            "OS: " + System.getProperty("os.name") + "\n" +
                            "Exiftool: " + exifTool.getVersion() + "\n" +
                            "Debug: " + debugCheckbox.isSelected() + "\n" +
                            "Dry run: " + dryRun + "\n" +
                            "-------------------------------------------------";
                    System.out.println(initialStateMessage);

                    ProcessingTask task = new ProcessingTask(
                            textArea,
                            dirFile,
                            exifTool,
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
