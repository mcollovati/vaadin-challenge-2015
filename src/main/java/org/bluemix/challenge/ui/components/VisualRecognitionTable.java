package org.bluemix.challenge.ui.components;

import com.vaadin.ui.Button;

import org.vaadin.viritin.fields.MTable;
import org.watson.visualrecognition.response.Image;
import org.watson.visualrecognition.response.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marco on 20/10/15.
 */
public class VisualRecognitionTable extends MTable<Label> {

    private Button.ClickListener onAction = event -> {
    };

    public VisualRecognitionTable() {
        super(Label.class);
        withProperties("labelName", "labelScore");
        withColumnHeaders("Label", "Score");
        setSelectable(true);
    }

    public void withImageResponse(Image response) {
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
