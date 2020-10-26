package cz.skodape.taskrunner.storage.template.model;

/**
 * String transformation rule.
 */
public enum StringTransformation {
    None,
    LowerCase,
    UpperCase;

    public static StringTransformation fromString(String value) {
        switch (value.toLowerCase()) {
            case "lowercase":
                return LowerCase;
            case "uppercase":
                return UpperCase;
            case "none":
            default:
                return None;
        }
    }

    public String transform(String value) {
        if (value == null) {
            return null;
        }
        switch (this) {
            case LowerCase:
                return value.toLowerCase();
            case UpperCase:
                return value.toUpperCase();
            case None:
            default:
                return value;
        }
    }

}
