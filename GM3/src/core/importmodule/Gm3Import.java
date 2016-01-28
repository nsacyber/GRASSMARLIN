package core.importmodule;

import core.dataexport.GmDataExportType;
import core.exec.Gm3ImportTask;
import core.exportdata.DataExporter;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * 09.15.2015 - CC - New...
 */
public class Gm3Import extends ImportItem  {
    public Gm3Import(String path) {
        super(path, Import.GM3, true);
    }
    @Override
    public void run() {
        try {
            Unmarshaller unmarshaller = DataExporter.DataExportContext.INSTANCE.getContext().createUnmarshaller();
            File file = new File(getPath());
            GmDataExportType gmDataExportType = (GmDataExportType)unmarshaller.unmarshal(file);
            Gm3ImportTask importTask = new Gm3ImportTask(this,gmDataExportType);
            getImporter().run(importTask);
        } catch (JAXBException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }

    }
}
