package com.github.addshore.facebook.data.image.exif;

import com.thebuzzmedia.exiftool.Format;
import java.util.List;
import static java.util.Collections.singletonList;

/**
 * Structure copied from StandardFormat of which StandardFormat.NUMERIC is the same as DEFAULT here.
 */
public enum CustomFormat implements Format {
    DEFAULT {
        @Override
        public List<String> getArgs() {
            return singletonList("-n");
        }
    },
    DEFAULT_OVERWRITE_ORIGINAL {
        @Override
        public List<String> getArgs() {
            List<String> list = new java.util.ArrayList<>(singletonList("-n"));
            list.add("-overwrite_original");
            return list;
        }
    },
}
