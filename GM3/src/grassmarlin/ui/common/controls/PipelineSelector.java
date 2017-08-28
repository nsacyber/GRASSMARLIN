package grassmarlin.ui.common.controls;

import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
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

    public PipelineSelector() {
        super();

        super.setConverter(new PipelineStringConverter());
    }

    public void setSession(final Session session) {
        if(session == null) {
            super.setBaseList(new ArrayList<>());
        } else {
            super.setBaseList(session.getPipelineTemplates());
        }
    }
}
