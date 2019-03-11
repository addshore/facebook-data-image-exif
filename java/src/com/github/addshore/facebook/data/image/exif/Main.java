package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import com.thebuzzmedia.exiftool.exceptions.UnsupportedFeatureException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;


public class Main extends Application {

    private String version = "0.9";
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
        final Label toolLabel = (Label) dataEntryView.getChildren().get(2);

        // dirInputGrid pain
        final GridPane dirInputGrid = (GridPane) dataEntryView.getChildren().get(1);
        dirInput = (TextField) dirInputGrid.getChildren().get(0);
        final Button dirInputBrowse = (Button) dirInputGrid.getChildren().get(1);

        // exiftoolGrid pain
        final GridPane toolInputGrid = (GridPane) dataEntryView.getChildren().get(3);
        toolInput = (TextField) toolInputGrid.getChildren().get(0);
        final Button toolInputBrowse = (Button) toolInputGrid.getChildren().get(1);

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
            dirInput.setPromptText( "Example: C:\\Users\\example\\downloads\\extracted-facebook-export" );
            toolInput.setPromptText( "Example: C:\\Users\\example\\downloads\\exiftool.exe" );

        } else {
            dirInput.setPromptText("Example: /path/to/extracted-facebook-export");
            toolInput.setPromptText("Example: /usr/bin/exiftool");
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
        dirInputBrowse.setOnAction(this.getBrowseButtonClickEventHandler(dirInput, JFileChooser.DIRECTORIES_ONLY));
        toolInputBrowse.setOnAction(this.getBrowseButtonClickEventHandler(toolInput, JFileChooser.FILES_ONLY));

        return new Scene(dataEntryView, 500, 250);
    }

    private EventHandler<ActionEvent> getBrowseButtonClickEventHandler( TextField input, int selectionMode ) {
        return new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent t){
                JFileChooser chooser = new JFileChooser();

                // Add listener on chooser to detect changes to selected file
                chooser.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY
                                .equals(evt.getPropertyName())) {
                            JFileChooser chooser = (JFileChooser)evt.getSource();
                            try{
                                File curFile = chooser.getSelectedFile();
                                input.setText(curFile.getPath());
                            } catch( Exception ignored ) {

                            }
                        }
                    }
                });

                chooser.setFileSelectionMode(selectionMode);
                chooser.showOpenDialog(null);
            }
        };
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
                    ObservableList<String> lines = FXCollections.observableArrayList();
                    ListView<String> listView = new ListView<>(lines);
                    listView.setEditable(false);

                    // Keep the list view scrolled to the bottom
                    lines.addListener((ListChangeListener<String>) c -> {
                        listView.scrollTo(listView.getItems().size() - 1 );
                    });

                    lines.add("Task is starting...");

                    // Make sure if the window is closed while task is still running, everything exits
                    Platform.setImplicitExit(true);

                    stage.setScene(new Scene(listView, 800, 500));
                    stage.show();


                    // Try to create a fancy pooled and stay open exiftool
                    ExifTool exifTool;
                    boolean stayOpen = true;
                    try {
                        ExifToolBuilder builder = new ExifToolBuilder();
                        builder.withPath( exiftoolFile );

                        // If we have more than one processor, use a pool strategy of that size
                        if( Runtime.getRuntime().availableProcessors() > 1 ) {
                            builder.withPoolSize( Runtime.getRuntime().availableProcessors() );
                        }

                        builder.enableStayOpen();
                        exifTool = builder.build();
                    }
                     catch (UnsupportedFeatureException ex) {
                         // Fallback to just a pooled tool
                         ExifToolBuilder builder = new ExifToolBuilder();
                         builder.withPath( exiftoolFile );

                         // If we have more than two processors, use a pool strategy
                         if( Runtime.getRuntime().availableProcessors() > 2 ) {
                             // But always leave 1 processor totally free
                             builder.withPoolSize( Runtime.getRuntime().availableProcessors() - 1 );
                         }

                         stayOpen = false;
                         exifTool = builder.build();
                     }

                    String initialStateMessage = "Version: " + version + "\n" +
                            "OS: " + System.getProperty("os.name") + "\n" +
                            "Exiftool: " + exifTool.getVersion() + "\n" +
                            "Exiftool Poolsize: " + Runtime.getRuntime().availableProcessors() + "\n" +
                            "Exiftool Stayopen: " + stayOpen + "\n" +
                            "Debug: " + debugCheckbox.isSelected() + "\n" +
                            "Dry run: " + dryRun + "\n" +
                            "-------------------------------------------------";
                    System.out.println(initialStateMessage);

                    ProcessingTask task = new ProcessingTask(
                            lines,
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
