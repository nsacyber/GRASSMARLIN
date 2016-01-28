package core.fingerprint;

import TemplateEngine.Data.FunctionalOperation;

/**
 * Operation is an interface used to call into the on-the-fly compiled fingerprint code.
 */
@FunctionalInterface
public interface Operation extends FunctionalOperation {
    /**
     * The signature for the FunctionInterface
     * @param t LogicalDataImportTask contains information protocol information used to filter these operations.
     * @param payload Reference to the ProxyBuffer which contains the raw bytes of the payload (not the rest of the packet).
     * @param cursor Cursor used to mark and carry the position in the payload the fingerprint is looking at.
     * @param ret Set of DetaDetails containing artifacts extracted and flags set within the fingerprint.
     */
    //public void apply(LogicalDataImportTask t, ProxyBuffer payload, Fingerprint.Cursor cursor, Set<DataDetails> ret);
}
