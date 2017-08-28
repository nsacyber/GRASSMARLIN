package grassmarlin.session.pipeline;

import grassmarlin.session.ImportItem;

public interface IDeferredProgress {
    ImportItem getImportSource();
    long getImportProgress();
}
