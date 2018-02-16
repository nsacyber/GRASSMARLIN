package iadgov.ping;

import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;

public class ConfigStage implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    @PreferenceDialog.Field(name="Timeout Threshold", accessorName="PingTimeoutMs", nullable = false)
    private Integer timeoutPingMs = 1000;
    @PreferenceDialog.Field(name="Requery Limit", accessorName="PingDebounceMins", nullable = false)
    private Integer debouncePingMins = 5;

    public ConfigStage() {

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Integer getPingTimeoutMs() {
        return this.timeoutPingMs;
    }
    public void setPingTimeoutMs(final Integer ms) {
        this.timeoutPingMs = ms;
    }

    public Integer getPingDebounceMins() {
        return this.debouncePingMins;
    }
    public void setPingDebounceMins(final Integer mins) {
        this.debouncePingMins = mins;
    }
}
