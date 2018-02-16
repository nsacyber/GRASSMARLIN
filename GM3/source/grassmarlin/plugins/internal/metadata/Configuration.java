package grassmarlin.plugins.internal.metadata;

import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;

public class Configuration implements Cloneable, Serializable {
    @PreferenceDialog.Field(name="IPv4 Geolocation", accessorName="Ipv4GeolocationEnabled", nullable = false)
    private Boolean isIpv4GeolocationEnabled;
    //@PreferenceDialog.Field(name="IPv6 Geolocation", accessorName="Ipv6GeolocationEnabled", nullable = false)
    private Boolean isIpv6GeolocationEnabled;
    @PreferenceDialog.Field(name="OUI Lookup", accessorName="OuiLookupEnabled", nullable=false)
    private Boolean isOuiLookupEnabled;

    public Configuration() {
        this.isIpv4GeolocationEnabled = true;
        this.isOuiLookupEnabled = true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Boolean getIpv4GeolocationEnabled() {
        return this.isIpv4GeolocationEnabled;
    }
    public void setIpv4GeolocationEnabled(final Boolean value) {
        this.isIpv4GeolocationEnabled = value;
    }

    public Boolean getIpv6GeolocationEnabled() {
        return this.isIpv6GeolocationEnabled;
    }
    public void setIpv6GeolocationEnabled(final Boolean value) {
        this.isIpv6GeolocationEnabled = value;
    }

    public Boolean getOuiLookupEnabled() {
        return this.isOuiLookupEnabled;
    }
    public void setOuiLookupEnabled(final Boolean value) {
        this.isOuiLookupEnabled = value;
    }
}
