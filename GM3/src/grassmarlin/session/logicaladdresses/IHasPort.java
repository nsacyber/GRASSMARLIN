package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

public interface IHasPort {
    int getPort();
    LogicalAddress<?> getAddressWithoutPort();
}
