package org.bluemix.challenge.ui;

import org.vaadin.viritin.fields.MTable;
import org.watson.visualrecognition.response.Image;
import org.watson.visualrecognition.response.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marco on 20/10/15.
 */
public class VisualRecognitionTable extends MTable<Label> {

    public VisualRecognitionTable() {
        super(Label.class);
        withProperties("labelName", "labelScore");
        withColumnHeaders("Label", "Score");
        setCaption("Visual recognition results");
    }

    void withImageResponse(Image response) {
        List<Label> labels = response.getLabels();
        if (labels == null) {
            labels = new ArrayList<>();
        }
        setBeans(labels);
        markAsDirty();
    }

    public void clearTable() {
        super.setBeans();
    }
}
