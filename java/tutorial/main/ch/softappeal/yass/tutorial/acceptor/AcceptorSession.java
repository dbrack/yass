package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.remote.AsyncService;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.Service;
import ch.softappeal.yass.remote.session.Connection;
import ch.softappeal.yass.remote.session.SessionWatcher;
import ch.softappeal.yass.remote.session.SimpleSession;
import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceKind;
import ch.softappeal.yass.tutorial.contract.PriceListener;
import ch.softappeal.yass.tutorial.contract.SystemException;
import ch.softappeal.yass.tutorial.contract.generic.GenericEchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.AsyncLogger;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;
import static ch.softappeal.yass.tutorial.contract.Config.INITIATOR;

public final class AcceptorSession extends SimpleSession {

    private final Set<Integer> subscribedInstrumentIds = Collections.synchronizedSet(new HashSet<>());

    @Override protected Server server() {
        final var interceptor = Interceptor.composite(
            UnexpectedExceptionHandler.INSTANCE,
            new Logger(this, Logger.Side.SERVER)
        );
        return new Server(
            new AsyncService(ACCEPTOR.instrumentService, InstrumentServiceImpl.INSTANCE, AsyncLogger.INSTANCE),
            new Service(ACCEPTOR.priceEngine, new PriceEngineImpl(subscribedInstrumentIds), interceptor),
            new Service(ACCEPTOR.echoService, EchoServiceImpl.INSTANCE, interceptor),
            new Service(ACCEPTOR.genericEchoService, GenericEchoServiceImpl.INSTANCE, interceptor)
        );
    }

    private final PriceListener priceListener;
    private final EchoService echoService;

    public AcceptorSession(final Connection connection, final Executor dispatchExecutor) {
        super(connection, dispatchExecutor);
        System.out.println("session " + this + " created");
        final var interceptor = new Logger(this, Logger.Side.CLIENT);
        priceListener = proxy(INITIATOR.priceListener, interceptor);
        echoService = proxy(INITIATOR.echoService, interceptor);
    }

    @Override protected void opened() throws InterruptedException {
        SessionWatcher.watchSession(dispatchExecutor, this, 60L, 2L, () -> echoService.echo("checkFromAcceptor")); // optional
        System.out.println("session " + this + " opened start");
        System.out.println("echo: " + echoService.echo("hello from acceptor"));
        try {
            echoService.echo("throwRuntimeException");
        } catch (final SystemException e) {
            System.out.println("echo: " + e.message);
        }
        final var random = new Random();
        while (!isClosed()) {
            final List<Price> prices = new ArrayList<>();
            for (final int subscribedInstrumentId : subscribedInstrumentIds.toArray(new Integer[0])) {
                if (random.nextBoolean()) {
                    prices.add(new Price(subscribedInstrumentId, random.nextInt(99) + 1, PriceKind.values()[random.nextInt(2)]));
                }
            }
            if (!prices.isEmpty()) {
                priceListener.newPrices(prices);
            }
            TimeUnit.MILLISECONDS.sleep(500L);
        }
        System.out.println("session " + this + " opened end");
    }

    @Override protected void closed(final @Nullable Exception exception) {
        System.out.println("session " + this + " closed: " + exception);
    }

    private static final AtomicInteger ID = new AtomicInteger(1);
    private final String id = String.valueOf(ID.getAndIncrement());
    @Override public String toString() {
        return id;
    }

}
