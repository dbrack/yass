package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.core.remote.Completer;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InstrumentServiceImplAsync implements InstrumentService {

    static final Map<Integer, Instrument> INSTRUMENTS;
    static {
        final List<String> names = List.of("IBM", "Google", "Apple", "Microsoft");
        final Map<Integer, Instrument> instruments = new HashMap<>(names.size());
        int index = 0;
        for (final String name : names) {
            final int id = index++;
            instruments.put(id, new Stock(id, name, true));
        }
        INSTRUMENTS = Collections.unmodifiableMap(instruments);
    }

    private InstrumentServiceImplAsync() {
        // disable
    }

    @Override public List<Instrument> getInstruments() {
        final Completer completer = Server.completer();
        new Thread(() -> completer.complete(new ArrayList<>(INSTRUMENTS.values()))).start(); // setting result asynchronously
        return null; // needed for compiler; returned result is not used
    }

    @Override public void showOneWay(final boolean testBoolean, final int testInt) {
        // empty
    }

    public static final InstrumentService INSTANCE = new InstrumentServiceImplAsync();

}
