package grassmarlin.ui.common.controls;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.ObservableListUnion;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import javafx.application.Platform;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Optional;

public class PipelineSelector extends FilteredComboBox<PipelineTemplate> {
    public class PipelineStringConverter extends StringConverter<PipelineTemplate> {
        public PipelineStringConverter() {
            super();
        }

        @Override
        public String toString(PipelineTemplate template) {
            return template != null ? template.getName() : null;
        }

        @Override
        public PipelineTemplate fromString(String name) {
            Optional<PipelineTemplate> template = PipelineSelector.this.getAllItems().stream()
                    .filter(t -> t.getName().equals(name))
                    .findFirst();

            return template.orElse(null);
        }
    }

    private final ObservableListWrapper<PipelineTemplate> templatesFromSession;
    private final ObservableListUnion<PipelineTemplate> templateList;
    private Session sessionCurrent;

    public PipelineSelector(final RuntimeConfiguration config) {
        super();

        this.templatesFromSession = new ObservableListWrapper<>(new ArrayList<>(1));
        this.templateList = new ObservableListUnion<>(
                config.getPipelineTemplates(),
                this.templatesFromSession
        );
        this.sessionCurrent = null;

        super.setConverter(new PipelineStringConverter());
        super.setBaseList(this.templateList.get());
    }

    public void setSession(final Session session) {
        if(this.sessionCurrent != null) {
            this.templatesFromSession.remove(this.sessionCurrent.getSessionDefaultTemplate());
        }
        this.sessionCurrent = session;
        if(session == null) {
            this.disableProperty().unbind();
            this.disableProperty().set(true);
            this.valueProperty().set(null);
        } else {
            this.disableProperty().bind(session.canSetPipeline().not());
            this.templatesFromSession.add(session.getSessionDefaultTemplate());
            //We need to wait for the list events to fire--changing the selection now will cause issues as the item we're selecting doesn't exist.
            Platform.runLater(() -> this.getSelectionModel().select(session.getSessionDefaultTemplate()));
        }
    }
}
