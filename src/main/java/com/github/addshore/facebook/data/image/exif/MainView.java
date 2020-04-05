package com.github.addshore.facebook.data.image.exif;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;

public class MainView {

    // Grids
    public final GridPane dataEntryView;
    public final GridPane dirInputGrid;
    public final GridPane toolInputGrid;
    public final GridPane optionsGrid;
    public final GridPane linksGrid;
    public final GridPane submitGrid;

    // Inputs
    public final TextField dirInput;
    public final Button dirInputBrowse;

    public final Label toolLabel;
    public final TextField toolInput;
    public final Button toolInputBrowse;

    public final CheckBox overwriteCheckbox;

    public final Label versionLabel;
    public final Hyperlink hyperLinkAddshore;
    public final Hyperlink hyperLinkExif;

    public final Button runButton;
    public final Button dryRunButton;
    public final CheckBox debugCheckbox;

    public MainView() throws IOException {
        dataEntryView = FXMLLoader.load(getClass().getResource("dataEntry.fxml"));

        // dirInputGrid pain
        dirInputGrid = (GridPane) dataEntryView.getChildren().get(1);
        dirInput = (TextField) dirInputGrid.getChildren().get(0);
        dirInputBrowse = (Button) dirInputGrid.getChildren().get(1);

        toolLabel = (Label) dataEntryView.getChildren().get(2);

        // exiftoolGrid pain
        toolInputGrid = (GridPane) dataEntryView.getChildren().get(3);
        toolInput = (TextField) toolInputGrid.getChildren().get(0);
        toolInputBrowse = (Button) toolInputGrid.getChildren().get(1);

        // Additional Options grid
        optionsGrid = (GridPane) dataEntryView.getChildren().get(7);
        overwriteCheckbox = (CheckBox) optionsGrid.getChildren().get(0);

        // Details grid pain
        linksGrid = (GridPane) dataEntryView.getChildren().get(4);
        versionLabel = (Label) linksGrid.getChildren().get(0);
        hyperLinkAddshore = (Hyperlink) linksGrid.getChildren().get(1);
        hyperLinkExif = (Hyperlink) linksGrid.getChildren().get(2);

        // Submission grid
        submitGrid = (GridPane) dataEntryView.getChildren().get(6);
        runButton = (Button) submitGrid.getChildren().get(0);
        dryRunButton = (Button) submitGrid.getChildren().get(1);
        debugCheckbox = (CheckBox) submitGrid.getChildren().get(2);
    }

}
