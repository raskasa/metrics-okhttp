package okhttp3;

import com.raskasa.metrics.okhttp.ConnectionInterceptor;
import com.raskasa.metrics.okhttp.WrappedEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class WrappedEventListenerFactory implements EventListener.Factory {

    private final List<EventListener.Factory> factories = new ArrayList<>();

    public WrappedEventListenerFactory(final EventListener.Factory rawFactory,
                                       final ConnectionInterceptor connectionInterceptor) {
        factories.add(rawFactory);
        factories.add(EventListener.factory(connectionInterceptor));
    }

    @Override
    public EventListener create(final Call call) {
        List<EventListener> listeners =
                factories.stream().map(factory -> factory.create(call)).collect(Collectors.toList());
        return new WrappedEventListener(listeners);
    }
}
