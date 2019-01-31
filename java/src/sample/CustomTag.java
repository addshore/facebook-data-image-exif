//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sample;

import com.thebuzzmedia.exiftool.Constants;
import com.thebuzzmedia.exiftool.Tag;

import java.util.regex.Pattern;

public enum CustomTag implements Tag {
    EXPOSURE("EXPOSURE", Type.STRING),
    FNUMBER("FNumber", Type.STRING),
    MODIFYDATE("ModifyDate", Type.STRING);

    private final String name;
    private final CustomTag.Type type;

    private CustomTag(String name, CustomTag.Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T> T parse(String value) {
        return type.parse(value);
    }

    @SuppressWarnings("unchecked")
    private enum Type {
        INTEGER {
            @Override
            public <T> T parse(String value) {
                return (T) Integer.valueOf(Integer.parseInt(value));
            }
        },

        LONG {
            @Override
            public <T> T parse(String value) {
                return (T) Long.valueOf(Long.parseLong(value));
            }
        },

        DOUBLE {
            @Override
            public <T> T parse(String value) {
                return (T) Double.valueOf(Double.parseDouble(value));
            }
        },

        STRING {
            @Override
            public <T> T parse(String value) {
                return (T) value;
            }
        },

        ARRAY {
            @Override
            public <T> T parse(String value) {
                return (T) value.split(Pattern.quote(Constants.SEPARATOR)); }
        };

        public abstract <T> T parse(String value);
    }
}
