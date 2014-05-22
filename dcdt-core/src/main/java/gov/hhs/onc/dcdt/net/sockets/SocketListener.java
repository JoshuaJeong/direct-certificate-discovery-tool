package gov.hhs.onc.dcdt.net.sockets;

import gov.hhs.onc.dcdt.beans.ToolLifecycleBean;
import java.io.Closeable;
import org.springframework.context.ApplicationContextAware;

public interface SocketListener<T extends Closeable, U extends SocketAdapter<T>, V extends Closeable, W extends ClientSocketAdapter<V>, X extends SocketRequest, Y extends SocketRequestProcessor<X>>
    extends ApplicationContextAware, ToolLifecycleBean {
}
