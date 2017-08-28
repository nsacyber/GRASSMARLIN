package grassmarlin.plugins.internal.fingerprint.manager.payload;


public enum Test {
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    EQ("=="),
    AND("&"),
    OR("|");

    private String display;
    Test(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return this.display;
    }

    @Override
    public String toString() {
        return this.display;
    }
}
