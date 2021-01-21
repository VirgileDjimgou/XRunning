package com.chichikolon.xrunning.run.io.file.importer;

class ImportParserException extends RuntimeException {
    public ImportParserException(String msg) {
        super(msg);
    }

    public ImportParserException(Exception e) {
        super(e);
    }
}
