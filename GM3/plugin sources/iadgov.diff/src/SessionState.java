package iadgov.diff;

import grassmarlin.session.Session;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.scene.control.MenuItem;

public class SessionState {
    final MenuItem miMarkAsBaseline;

    SessionState(final Session session) {
        this.miMarkAsBaseline = new ActiveMenuItem("Mark as Baseline", action -> {

        });
    }
}
