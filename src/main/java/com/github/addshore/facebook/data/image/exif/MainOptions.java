package com.github.addshore.facebook.data.image.exif;

public class MainOptions {

    private final Boolean debug;
    private final Boolean dry;
    private final Boolean overwriteOriginals;

    public MainOptions(
            Boolean debug,
            Boolean dry,
            Boolean overwriteOriginals
    ) {
        this.debug = debug;
        this.dry = dry;
        this.overwriteOriginals = overwriteOriginals;
    }

    public Boolean isDryMode() {
        return dry;
    }

    public Boolean isDebugMode() {
        return debug;
    }

    public Boolean shouldOverwriteOriginals() {
        return overwriteOriginals;
    }

}
