package org.bluemix.challenge.ui;

import org.vaadin.viritin.fields.MTable;
import org.watson.visualrecognition.response.Image;
import org.watson.visualrecognition.response.Label;

/**
 * Created by marco on 20/10/15.
 */
public class VisualRecognitionTable extends MTable<Label> {

    public VisualRecognitionTable() {
        super(Label.class);
        withColumnHeaders("Label", "Score");
        withProperties("labelName", "labelScore");
    }

    void withImageResponse(Image response) {
        setCaption(response.getImageName());
        setBeans(response.getLabels());
    }
}
