package gov.nist.toolkit.xdstools2.client.abstracts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

import javax.inject.Inject;

/**
 * Singleton
 */
public class ToolMenu implements IsWidget /*, ValueChangeHandler<String> */{
    private FlowPanel panel = new FlowPanel();

    private PlaceController placeController;

    @Inject
    public ToolMenu(PlaceController placeController) {
        this.placeController = placeController;

        //build();

    }

    private void build() {
//        GWT.log("Build Menu");
//        panel.add(new HTML("<h2>Menu</h2>"));
//        add("Toy", new Toy());
//        add("Ball", new Toy("Ball"));
//        add("GetDocuments", new GetDocuments());
    }

    private void add(String placeName, final Place place) {
        Hyperlink h = new Hyperlink(placeName, placeName);
        h.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                GWT.log("GoTo " + place.getClass().getName());
                placeController.goTo(place);
            }
        });
        panel.add(h);
    }

    @Override
    public Widget asWidget() {
        return panel;
    }
}
